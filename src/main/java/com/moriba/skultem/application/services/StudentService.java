package com.moriba.skultem.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.application.usecase.GetFeeDetailUsecase;
import com.moriba.skultem.application.usecase.ResolveAcademicYearUseCase;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final GetFeeDetailUsecase getFeeDetailUsecase;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public Page<StudentDTO> search(String value, int page, int size, String schoolId, String academicYearId) {
        Pageable pageable = PageableMapper.toPage(page, size);

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        // Scoped to students enrolled for that year (any status) - see StudentJpaRepository#search -
        // so the enrollment lookup below (just for display: class name, etc.) is always guaranteed to
        // find one and never needs the "fall back to their last known enrollment" trick that
        // ListStudentBySchoolUseCase still needs for its broader, unscoped listing.
        return studentRepo.search(value, schoolId, academicYear.getId(), pageable).map(student -> {
            Enrollment enrollment = enrollmentRepo
                    .findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(), schoolId)
                    .orElse(null);
            var feeDetail = getFeeDetailUsecase.execute(schoolId, student.getId());

            return StudentMapper.toDTO(student, enrollment, feeDetail);
        });
    }
}
