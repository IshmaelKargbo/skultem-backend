package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionRequestDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PromotionRequestMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.model.PromotionRequestItem;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.rest.dto.PromotionItemInputDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The class master's step of the promotion flow: mark every active student in their class session
 * as PROMOTE or REPEAT with a remark, and submit for admin/proprietor review. Nothing moves yet -
 * that happens once {@link ApprovePromotionRequestUseCase} runs.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SubmitPromotionRequestUseCase {

    private final ClassSessionRepository classSessionRepo;
    private final ClassMasterRepository classMasterRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final PromotionRequestRepository promotionRequestRepo;
    private final StreamRepository streamRepo;
    private final ValidateAcademicYearTermsUseCase validateAcademicYearTermsUseCase;
    private final ValidateClassAssessmentsCompletedUseCase validateClassAssessmentsCompletedUseCase;
    private final ValidateNextAcademicYearUseCase validateNextAcademicYearUseCase;
    private final GetPromotionConfigUseCase getPromotionConfigUseCase;
    private final ApprovePromotionRequestUseCase approvePromotionRequestUseCase;

    @AuditLogAnnotation(action = "PROMOTION_SUBMITTED")
    public PromotionRequestDTO execute(String schoolId, String sessionId, String note,
            List<PromotionItemInputDTO> input) {
        var session = classSessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var clazz = session.getClazz();
        var academicYear = session.getAcademicYear();

        var activeAcademicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new RuleException("Active academic year not found"));

        if (!activeAcademicYear.getId().equals(academicYear.getId())) {
            throw new RuleException("This class session is not part of the active academic year");
        }

        validateAcademicYearTermsUseCase.execute(schoolId, academicYear.getId());
        validateClassAssessmentsCompletedUseCase.execute(schoolId, clazz.getId(), academicYear.getId());

        var master = classMasterRepo.findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(sessionId)
                .orElseThrow(() -> new RuleException("This class session has no active class master"));

        var config = getPromotionConfigUseCase.resolve(schoolId);

        var section = session.getSection();
        var stream = session.getStream();

        var activeEnrollments = enrollmentRepo.findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(
                clazz.getId(), section.getId(), stream != null ? stream.getId() : null, academicYear.getId(),
                schoolId);

        if (activeEnrollments.isEmpty()) {
            throw new RuleException("There are no active students in this class to promote");
        }

        Map<String, Enrollment> byEnrollmentId = new HashMap<>();
        for (var enrollment : activeEnrollments) {
            byEnrollmentId.put(enrollment.getId(), enrollment);
        }

        if (input == null || input.size() != activeEnrollments.size()) {
            throw new RuleException("A decision is required for every active student in this class");
        }

        // Streams (Science/Arts/Commercial) only exist at SSS level - a JSS/Primary session never
        // has one. Promoting into an SSS next class means placing each promoted student into a
        // stream for the first time, which the class master must choose per student.
        boolean promotionNeedsStream = stream == null && clazz.getNextClass() != null
                && clazz.getNextClass().getLevel() == Level.SSS;

        boolean hasPromote = false;
        List<PromotionRequestItem> items = new ArrayList<>();
        for (var line : input) {
            var enrollment = byEnrollmentId.remove(line.enrollmentId());
            if (enrollment == null || !enrollment.getStudent().getId().equals(line.studentId())) {
                throw new RuleException("One of the submitted students is not on this class's active roster");
            }

            PromotionRequestItem.Outcome outcome;
            try {
                outcome = PromotionRequestItem.Outcome.valueOf(line.outcome());
            } catch (IllegalArgumentException e) {
                throw new RuleException("Invalid outcome: " + line.outcome());
            }

            if (outcome == PromotionRequestItem.Outcome.PROMOTE) {
                hasPromote = true;
            }

            if (config.isRequireRemarkForPromote() && (line.remark() == null || line.remark().isBlank())) {
                throw new RuleException(
                        "A remark is required for every student (" + enrollment.getStudent().getName() + ")");
            }

            if (outcome == PromotionRequestItem.Outcome.REPEAT) {
                long timesRepeated = enrollmentRepo.countByStudentIdAndClassIdAndSchoolIdAndStatus(
                        enrollment.getStudent().getId(), clazz.getId(), schoolId, Enrollment.Status.REPEATED);

                if (timesRepeated >= config.getMaxRepeatCount()) {
                    throw new RuleException(enrollment.getStudent().getName()
                            + " has already repeated this class the maximum number of times allowed ("
                            + config.getMaxRepeatCount() + "). Choose a different outcome or update the school's promotion rules.");
                }
            }

            Stream targetStream = null;
            if (outcome == PromotionRequestItem.Outcome.PROMOTE && promotionNeedsStream) {
                if (line.targetStreamId() == null || line.targetStreamId().isBlank()) {
                    throw new RuleException("A stream is required for " + enrollment.getStudent().getName()
                            + " - this class promotes into an SSS class");
                }

                targetStream = streamRepo.findByIdAndSchoolId(line.targetStreamId(), schoolId)
                        .orElseThrow(() -> new RuleException("Selected stream not found"));
            }

            items.add(PromotionRequestItem.create(UUID.randomUUID().toString(), enrollment.getStudent(), enrollment,
                    outcome, line.remark(), targetStream));
        }

        if (!byEnrollmentId.isEmpty()) {
            throw new RuleException("A decision is required for every active student in this class");
        }

        if (hasPromote && !Boolean.TRUE.equals(clazz.getTerminal()) && clazz.getNextClass() == null) {
            throw new RuleException(
                    "This class has no next class configured. Set one from Class Management before promoting.");
        }

        // A terminal class's PROMOTE outcome graduates the student and needs no target session - every
        // other outcome (REPEAT, or PROMOTE out of a non-terminal class) lands a student in next year,
        // so that year has to exist before this request can go anywhere.
        boolean needsTargetYear = items.stream()
                .anyMatch(item -> item.getOutcome() == PromotionRequestItem.Outcome.REPEAT
                        || (item.getOutcome() == PromotionRequestItem.Outcome.PROMOTE
                                && !Boolean.TRUE.equals(clazz.getTerminal())));

        if (needsTargetYear) {
            validateNextAcademicYearUseCase.execute(schoolId, academicYear.getId());
        }

        var existing = promotionRequestRepo.findOpenBySessionIdAndAcademicYearIdAndSchoolId(sessionId,
                academicYear.getId(), schoolId);

        PromotionRequest request;
        if (existing.isPresent()) {
            request = existing.get();
            if (request.isPending()) {
                throw new RuleException("A promotion request for this class is already awaiting review");
            }
            request.resubmit(items, note);
        } else {
            request = PromotionRequest.create(UUID.randomUUID().toString(), schoolId, session, master, academicYear,
                    items, note);
        }

        promotionRequestRepo.save(request);

        if (!config.isRequireApproval()) {
            return approvePromotionRequestUseCase.execute(schoolId, request.getId(),
                    "Auto-approved - this school's promotion rules don't require admin review");
        }

        return PromotionRequestMapper.toDTO(request);
    }
}
