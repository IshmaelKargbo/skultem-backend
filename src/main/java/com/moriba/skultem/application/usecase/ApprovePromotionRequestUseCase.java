package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionRequestDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PromotionRequestMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.PromotionRequestItem;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The admin/proprietor's step of the promotion flow: approving a class master's submission runs
 * "the copy process" - every PROMOTE student gets a new enrollment in the class's next class (or
 * graduates, if the class is terminal), every REPEAT student gets a new enrollment back in the
 * same class, both for the next academic year, and the old enrollment is closed out.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ApprovePromotionRequestUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApprovePromotionRequestUseCase.class);

    private final PromotionRequestRepository promotionRequestRepo;
    private final ClassSessionRepository classSessionRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentCreationService enrollmentCreationService;
    private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;
    private final ApplyApplicableFeesToEnrollmentUseCase applyApplicableFeesToEnrollmentUseCase;
    private final CarryForwardClassSessionSetupUseCase carryForwardClassSessionSetupUseCase;
    private final EnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    public PromotionRequestDTO execute(String schoolId, String requestId, String note) {
        return execute(schoolId, requestId, note, null);
    }

    /**
     * @param allowToPassEnrollmentIds enrollments the reviewing admin is overriding from REPEAT to
     *                                 PROMOTE right here, instead of returning the whole request to
     *                                 the class master just to change one or two students.
     */
    @AuditLogAnnotation(action = "PROMOTION_APPROVED")
    public PromotionRequestDTO execute(String schoolId, String requestId, String note,
            List<String> allowToPassEnrollmentIds) {
        var request = promotionRequestRepo.findByIdAndSchoolId(requestId, schoolId)
                .orElseThrow(() -> new NotFoundException("Promotion request not found"));

        if (allowToPassEnrollmentIds != null && !allowToPassEnrollmentIds.isEmpty()) {
            Set<String> overrides = Set.copyOf(allowToPassEnrollmentIds);
            request.getItems().stream()
                    .filter(item -> overrides.contains(item.getEnrollment().getId())
                            && item.getOutcome() == PromotionRequestItem.Outcome.REPEAT)
                    .forEach(PromotionRequestItem::allowToPass);
        }

        var session = request.getSession();
        var clazz = session.getClazz();
        var section = session.getSection();
        var stream = session.getStream();
        boolean terminal = Boolean.TRUE.equals(clazz.getTerminal());

        boolean needsTargetYear = request.getItems().stream()
                .anyMatch(item -> item.getOutcome() == PromotionRequestItem.Outcome.REPEAT
                        || (item.getOutcome() == PromotionRequestItem.Outcome.PROMOTE && !terminal));

        AcademicYear year = needsTargetYear
                ? academicYearRepo.findNextBySchool(schoolId, request.getAcademicYear().getEndDate())
                        .orElseThrow(() -> new RuleException(
                                "No upcoming academic year is set up yet. Create next year's academic year before approving."))
                : null;

        int promoted = 0;
        int repeated = 0;

        for (var item : request.getItems()) {
            var enrollment = item.getEnrollment();
            var student = item.getStudent();

            if (item.getOutcome() == PromotionRequestItem.Outcome.PROMOTE && terminal) {
                student.graduate();
                studentRepo.save(student);
                enrollment.promote();
                enrollmentRepo.save(enrollment);
                promoted++;
                continue;
            }

            Clazz destinationClazz = item.getOutcome() == PromotionRequestItem.Outcome.PROMOTE ? clazz.getNextClass()
                    : clazz;

            if (destinationClazz == null) {
                throw new RuleException(
                        "This class has no next class configured. Set one from Class Management before approving.");
            }

            // A REPEAT always stays in the exact same class/section/stream. A PROMOTE normally carries
            // the source stream forward too, except when moving from a streamless class (JSS/Primary)
            // into an SSS class, where the class master chose a stream for this student at submission.
            Stream destinationStream = item.getOutcome() == PromotionRequestItem.Outcome.PROMOTE
                    && item.getTargetStream() != null ? item.getTargetStream() : stream;

            if (destinationClazz.getLevel() == Level.SSS && destinationStream == null) {
                throw new RuleException(
                        student.getName() + " has no stream selected for " + destinationClazz.getName()
                                + ". Return this request so the class master can pick one.");
            }

            var targetSession = findOrCreateTargetSession(schoolId, destinationClazz, year, section, destinationStream);

            try {
                var next = enrollmentCreationService.create(schoolId, student, destinationClazz, section, year,
                        destinationStream);

                try {
                    provisionStudentAssessmentsUseCase.execute(next);
                } catch (RuleException | NotFoundException e) {
                    log.warn("Could not provision assessments for promoted student {}: {}", student.getId(),
                            e.getMessage());
                }

                // Charges the student for whatever fee structures already exist for the new year/class -
                // a no-op if the school hasn't set any up yet (they'll be picked up when it's created, see
                // ApplyApplicableFeesToEnrollmentUseCase).
                try {
                    applyApplicableFeesToEnrollmentUseCase.execute(next);
                } catch (RuleException | NotFoundException e) {
                    log.warn("Could not apply fee structures for promoted student {}: {}", student.getId(),
                            e.getMessage());
                }
            } catch (AlreadyExistsException e) {
                // Student already has an enrollment in the target class/session - just close out the old one.
                log.info("Student {} already enrolled in target session {}, skipping duplicate", student.getId(),
                        targetSession.getId());
            }

            if (item.getOutcome() == PromotionRequestItem.Outcome.PROMOTE) {
                enrollment.promote();
                promoted++;
            } else {
                enrollment.repeat();
                repeated++;
            }
            // The source enrollment's new status (PROMOTED/REPEATED) has to be persisted explicitly -
            // it's a separate aggregate from the new enrollment created above, which saves itself.
            // Without this the old enrollment silently stays ACTIVE forever, as if nothing happened.
            enrollmentRepo.save(enrollment);
        }

        request.approve(note, promoted, repeated);
        promotionRequestRepo.save(request);

        logActivityUseCase.log(
                schoolId,
                ActivityType.CLASS,
                "Class promotion approved",
                session.getName() + " - " + promoted + " promoted, " + repeated + " repeating",
                null,
                session.getId());

        return PromotionRequestMapper.toDTO(request);
    }

    private ClassSession findOrCreateTargetSession(String schoolId, Clazz clazz, AcademicYear year, Section section,
            Stream stream) {
        Optional<ClassSession> found = stream != null
                ? classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(clazz.getId(),
                        year.getId(), section.getId(), stream.getId(), schoolId)
                : classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndSchoolId(clazz.getId(), year.getId(),
                        section.getId(), schoolId);

        return found.orElseGet(() -> {
            var created = ClassSession.create(rg.generate("CLASS_SESSION", "CSN"), schoolId, clazz, stream, section,
                    year);
            classSessionRepo.save(created);
            // New session, blank slate: pull forward last year's class master and teacher/subject
            // assignments so the promoted/repeating students landing here can get their assessments
            // provisioned right away instead of silently missing a teacher-subject to attach to.
            carryForwardClassSessionSetupUseCase.execute(created);
            return created;
        });
    }
}
