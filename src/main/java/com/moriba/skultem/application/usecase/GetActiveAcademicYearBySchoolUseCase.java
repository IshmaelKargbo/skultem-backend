package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetActiveAcademicYearBySchoolUseCase {
    private final AcademicYearRepository repo;

    public AcademicYear execute(String schoolId) {
        return repo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
    }
}
