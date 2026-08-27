package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionRosterDTO;
import com.moriba.skultem.application.dto.RosterStudentDTO;
import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.StreamMapper;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The active roster a class master sees when opening the promotion review screen for their
 * session, along with whether the class is currently eligible for promotion.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GetPromotionRosterUseCase {

    private final ClassSessionRepository classSessionRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final StreamRepository streamRepo;
    private final ComputeEnrollmentYearAverageUseCase computeEnrollmentYearAverageUseCase;
    private final GetPromotionConfigUseCase getPromotionConfigUseCase;
    private final ValidateAcademicYearTermsUseCase validateAcademicYearTermsUseCase;
    private final ValidateClassAssessmentsCompletedUseCase validateClassAssessmentsCompletedUseCase;
    private final ValidateNextAcademicYearUseCase validateNextAcademicYearUseCase;

    public PromotionRosterDTO execute(String schoolId, String sessionId) {
        var session = classSessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var clazz = session.getClazz();
        var academicYear = session.getAcademicYear();
        var section = session.getSection();
        var stream = session.getStream();

        String targetClassName = Boolean.TRUE.equals(clazz.getTerminal())
                ? "Graduation"
                : (clazz.getNextClass() != null ? clazz.getNextClass().getName() : null);

        String ineligibleReason = null;
        try {
            // A terminal class's PROMOTE outcome is graduation, which needs no next year - but it may
            // still gain a next year requirement once the class master picks REPEAT for someone, so
            // this is a best-effort preview; SubmitPromotionRequestUseCase re-checks against the
            // actual chosen outcomes.
            if (!Boolean.TRUE.equals(clazz.getTerminal())) {
                validateNextAcademicYearUseCase.execute(schoolId, academicYear.getId());
            }

            validateAcademicYearTermsUseCase.execute(schoolId, academicYear.getId());
            validateClassAssessmentsCompletedUseCase.execute(schoolId, clazz.getId(), academicYear.getId());

            if (targetClassName == null) {
                ineligibleReason = "This class has no next class configured. Set one from Class Management first.";
            }
        } catch (RuleException e) {
            ineligibleReason = e.getMessage();
        }

        var enrollments = enrollmentRepo.findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(
                clazz.getId(), section.getId(), stream != null ? stream.getId() : null, academicYear.getId(),
                schoolId);

        Integer minPassMark = getPromotionConfigUseCase.resolve(schoolId).getMinPassMark();

        var students = enrollments.stream()
                .map(e -> {
                    Double average = computeEnrollmentYearAverageUseCase.execute(schoolId, e.getId());
                    String suggestedOutcome = average != null && minPassMark != null
                            ? (average >= minPassMark ? "PROMOTE" : "REPEAT")
                            : null;

                    return new RosterStudentDTO(e.getStudent().getId(), e.getId(), e.getStudent().getName(),
                            e.getStudent().getAdmissionNumber(), average, suggestedOutcome);
                })
                .toList();

        // Streams only exist at SSS level - promoting out of a streamless class (JSS/Primary) into an
        // SSS next class means picking a stream per student for the first time.
        boolean requiresStreamSelection = stream == null && clazz.getNextClass() != null
                && clazz.getNextClass().getLevel() == Level.SSS;

        var availableStreams = requiresStreamSelection
                ? streamRepo.findBySchool(schoolId, Pageable.unpaged()).getContent().stream()
                        .map(StreamMapper::toDTO)
                        .toList()
                : List.<StreamDTO>of();

        if (requiresStreamSelection && availableStreams.isEmpty() && ineligibleReason == null) {
            ineligibleReason = "No streams are set up yet. Create at least one stream before promoting into an SSS class.";
        }

        if (students.isEmpty()) {
            // Distinguish "this class session was never populated" from "everyone in it has already
            // been promoted/repeated/left" - the latter looks identical from an empty active roster
            // alone and reads as a bug otherwise.
            boolean everHadStudents = enrollmentRepo.existsAnyByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(
                    clazz.getId(), section.getId(), stream != null ? stream.getId() : null, academicYear.getId(),
                    schoolId);

            ineligibleReason = everHadStudents
                    ? "Every student in this class has already been promoted, repeated, or left for this year."
                    : "There are no active students in this class";
        }

        return new PromotionRosterDTO(session.getName(), targetClassName,
                ineligibleReason == null && !students.isEmpty(),
                ineligibleReason, requiresStreamSelection, availableStreams, students);
    }
}
