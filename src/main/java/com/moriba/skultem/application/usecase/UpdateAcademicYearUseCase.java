package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AcademicYearMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Editing an academic year's own name/dates is only safe while it's neither the school's current
 * year nor already wrapped up - once it's active, terms/fees/sessions are built against its dates,
 * and once it's closed, it's history.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateAcademicYearUseCase {

    private final AcademicYearRepository repo;

    @AuditLogAnnotation(action = "ACADEMIC_YEAR_UPDATED")
    public AcademicYearDTO execute(String schoolId, String id, String name, LocalDate startDate, LocalDate endDate) {
        var year = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (year.isActive()) {
            throw new RuleException("The active academic year can't be edited");
        }
        if (year.getStatus() == AcademicYear.Status.CLOSED) {
            throw new RuleException("A closed academic year can't be edited");
        }

        if (!endDate.isAfter(startDate)) {
            throw new RuleException("End date must be after start date");
        }

        if (!year.getName().equalsIgnoreCase(name) && repo.existsByNameAndSchool(name, schoolId)) {
            throw new AlreadyExistsException("academic year already exist");
        }

        year.update(name, startDate, endDate);
        repo.save(year);

        return AcademicYearMapper.toDTO(year);
    }
}
