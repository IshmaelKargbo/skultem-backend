package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentBySchoolUseCase {
    private final StudentRepository repo;
    private final EnrollmentRepository enrollmentRepo;
    private final GetFeeDetailUsecase getFeeDetailUsecase;
    private final AcademicYearRepository academicYearRepo;

    public Page<StudentDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = size > 0 ? PageRequest.of(page, size) : Pageable.unpaged();

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        return repo.findBySchoolId(schoolId, pageable).map(student -> {
            Enrollment enrollment = enrollmentRepo
                    .findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("Enrollment not found"));
            var feeDetail = getFeeDetailUsecase.execute(schoolId, student.getId());
            return StudentMapper.toDTO(student, enrollment, feeDetail);
        });
    }
}
