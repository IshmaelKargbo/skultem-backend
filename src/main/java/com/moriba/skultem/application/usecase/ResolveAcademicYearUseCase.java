package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

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
