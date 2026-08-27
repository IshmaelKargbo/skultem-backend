package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.model.Enrollment;
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
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public Page<StudentDTO> execute(String schoolId, String academicYearId, int page, int size) {
        Pageable pageable = size > 0 ? PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")) : Pageable.unpaged();

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        // Only mask a year-rollover gap with the student's most recent enrollment when nobody asked
        // for a specific year - if the admin explicitly browsed to a past/future year, a student with
        // no enrollment that year should show as such, not silently display a different year's class.
        boolean explicitYear = academicYearId != null && !academicYearId.isBlank();

        return repo.findBySchoolId(schoolId, pageable).map(student -> {
            var found = enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(),
                    schoolId);
            Enrollment enrollment = (explicitYear ? found
                    : found.or(() -> enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(student.getId(),
                            schoolId)))
                    .orElse(null);
            var feeDetail = getFeeDetailUsecase.execute(schoolId, student.getId());

            return StudentMapper.toDTO(student, enrollment, feeDetail);
        });
    }
}
