package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves which academic year a *read* should be scoped to: the explicitly requested one (the
 * frontend's header year switcher sends its selection as {@code academicYearId} on every GET), or
 * the school's active year when nothing was requested - which is also what every caller did before
 * this existed, so passing {@code null}/blank keeps existing behavior unchanged.
 * <p>
 * Only for reads. Anything that writes (recording a payment, marking attendance, assigning a
 * subject, promoting a class, ...) must keep resolving the *actual* active year directly via
 * {@link AcademicYearRepository#findActiveBySchool} - never this - so a browsed-to past/future year
 * can't accidentally become the target of a real action.
 */
@Service
@RequiredArgsConstructor
public class ResolveAcademicYearUseCase {

    private final AcademicYearRepository repo;

    public AcademicYear execute(String schoolId, String academicYearId) {
        if (academicYearId != null && !academicYearId.isBlank()) {
            return repo.findByIdAndSchoolId(academicYearId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Academic year not found"));
        }

        return repo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
    }
}
