package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetStudentUseCase {
    private final StudentRepository repo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;

    public StudentDTO execute(String id, String schoolId) {
        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        var student = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("student not found"));
        var enrollment = enrollmentRepo
                .findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(), schoolId)
                .orElseThrow(() -> new NotFoundException("enrollment not found"));

        return StudentMapper.toDTO(student, enrollment);
    }
}
