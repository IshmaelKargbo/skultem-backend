package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * A soft delete - the row (and anything it's already accumulated: terms, fee structures, ...)
 * stays put, it's just marked {@link AcademicYear.Status#DELETED} so it drops out of every normal
 * listing and lookup. Only ever allowed for a year that's neither the school's current year nor
 * already closed.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteAcademicYearUseCase {

    private final AcademicYearRepository repo;

    @AuditLogAnnotation(action = "ACADEMIC_YEAR_DELETED")
    public void execute(String schoolId, String id) {
        var year = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (year.isActive()) {
            throw new RuleException("The active academic year can't be deleted");
        }
        if (year.getStatus() == AcademicYear.Status.CLOSED) {
            throw new RuleException("A closed academic year can't be deleted");
        }
        if (repo.existsAsNextYear(schoolId, id)) {
            throw new RuleException("This year is set as another year's next year - unassign that link first");
        }

        year.markDeleted();
        repo.save(year);
    }
}
