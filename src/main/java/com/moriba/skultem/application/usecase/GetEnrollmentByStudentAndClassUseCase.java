package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.EnrollmentMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetEnrollmentByStudentAndClassUseCase {

    private final EnrollmentRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public EnrollmentDTO execute(String studentId, String school) {
        AcademicYear academicYear = academicYearRepo.findActiveBySchool(school)
                .orElseThrow(() -> new NotFoundException("academic year not found"));

        var res = repo.findByStudentAndAcademicYearAndSchoolId(studentId, academicYear.getId(), school)
                .orElseThrow(() -> new NotFoundException("enrollment not found"));
        return EnrollmentMapper.toDTO(res);

    }
}
