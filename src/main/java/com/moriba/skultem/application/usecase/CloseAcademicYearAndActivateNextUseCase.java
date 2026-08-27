package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The final, deliberate step of the promotion flow: once every class has been promoted, an admin
 * closes out the current academic year and activates the next one.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CloseAcademicYearAndActivateNextUseCase {

    private final AcademicYearRepository academicYearRepo;
    private final TermRepository termRepository;
    private final GetPromotionProgressUseCase getPromotionProgressUseCase;
    private final ActivateTermUseCase activateTermUseCase;
    private final CreateClassSessionsForAcademicYearUseCase createClassSessionsForAcademicYearUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ACADEMIC_YEAR_CLOSED")
    public void execute(String schoolId) {
        var progress = getPromotionProgressUseCase.execute(schoolId);

        if (progress.pendingSessions() > 0) {
            throw new RuleException(
                    "Not every class has been promoted yet (" + progress.pendingSessions()
                            + " class(es) still pending). Promote them before closing the year.");
        }

        if (progress.outstandingPlatformFee().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuleException(
                    "This school still owes " + progress.outstandingPlatformFee()
                            + " in platform fees. Settle the outstanding balance before closing the year.");
        }

        if (!progress.readyToCloseYear()) {
            throw new RuleException("This academic year isn't ready to close yet.");
        }

        var activeYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new RuleException("Active academic year not found"));

        var nextYear = academicYearRepo.findNextBySchool(schoolId, activeYear.getEndDate())
                .orElseThrow(() -> new RuleException("No upcoming academic year is set up yet."));

        activeYear.lock();
        academicYearRepo.save(activeYear);

        academicYearRepo.deactivateAllBySchool(schoolId);
        nextYear.open();
        nextYear.setActive(true);
        academicYearRepo.save(nextYear);

        // Activating the year alone leaves every one of its terms UPCOMING - kick off the
        // assessment cycle by activating Term 1, if it's already been templated in.
        termRepository.findByTernNumberAndAcademicYearIdAndSchoolId(1, nextYear.getId(), schoolId)
                .ifPresent(firstTerm -> activateTermUseCase.execute(schoolId, firstTerm.getId()));

        // A class only gets a session in the new year once a student is promoted into it -
        // pre-create every class's sessions so admins can set up class masters/subjects for a
        // class nobody has been promoted to yet.
        createClassSessionsForAcademicYearUseCase.execute(schoolId, nextYear.getId());

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Academic year closed",
                activeYear.getName() + " closed, " + nextYear.getName() + " is now active",
                null,
                nextYear.getId());
    }
}
