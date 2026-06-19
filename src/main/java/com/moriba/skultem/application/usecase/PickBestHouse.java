package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PickBestHouse {
    
    private final EnrollmentRepository enrollmentRepo;
    private final AcademicYearRepository academicYearRepo;

    public void assignHouse(String school, String classId) {
        var academic = academicYearRepo.findActiveBySchool(school).orElseThrow(() -> new NotFoundException("no active academic year found"));
        var enrollments = enrollmentRepo.findAllByClassAndAcademicAndSchoolId(classId, academic.getId(), school, Pageable.unpaged());

        
    }
}
