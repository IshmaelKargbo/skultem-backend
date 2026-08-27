package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Gate for the promotion flow: a class can only be promoted (its students moved into a target
 * class session) once the school has configured the academic year that follows the one being
 * promoted out of. Checked up front - at roster review and submission - so the class master finds
 * out before doing the review work, instead of the request failing deep inside approval.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ValidateNextAcademicYearUseCase {

    private final AcademicYearRepository academicYearRepo;

    public void execute(String schoolId, String academicYearId) {
        var academicYear = academicYearRepo.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        boolean configured = academicYearRepo.findNextBySchool(schoolId, academicYear.getEndDate()).isPresent();

        if (!configured) {
            throw new RuleException(
                    "Set up next academic year before promoting " + academicYear.getName() + "'s students");
        }
    }
}
