package com.moriba.skultem.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.application.usecase.GetFeeDetailUsecase;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final GetFeeDetailUsecase getFeeDetailUsecase;
    private final AcademicYearRepository academicYearRepo;

    public Page<StudentDTO> search(String value, int page, int size, String schoolId) {
        Pageable pageable = PageableMapper.toPageable(page, size);

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
        return studentRepo.search(value, schoolId, pageable).map(student -> {
            Enrollment enrollment = enrollmentRepo
                    .findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("Enrollment not found"));
            var feeDetail = getFeeDetailUsecase.execute(schoolId, student.getId());

            return StudentMapper.toDTO(student, enrollment, feeDetail);
        });
    }
}
