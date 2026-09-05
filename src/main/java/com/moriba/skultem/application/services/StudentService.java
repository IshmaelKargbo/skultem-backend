package com.moriba.skultem.application.services;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // s.<field>", so an unchecked client value would let someone probe/sort by arbitrary entity
    // fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("givenNames", "familyName", "admissionNumber",
            "createdAt");

    public Page<StudentDTO> search(String value, int page, int size, String schoolId, String academicYearId) {
        return search(value, page, size, schoolId, academicYearId, null, null, null);
    }

    public Page<StudentDTO> search(String value, int page, int size, String schoolId, String academicYearId,
            String classId, String sortBy, String direction) {
        Pageable pageable = PageableMapper.toPage(page, size, resolveSort(sortBy, direction));

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        // Scoped to students enrolled for that year (any status) - see StudentJpaRepository#search -
        // so the enrollment lookup below (just for display: class name, etc.) is always guaranteed to
        // find one and never needs the "fall back to their last known enrollment" trick that
        // ListStudentBySchoolUseCase still needs for its broader, unscoped listing.
        return studentRepo.search(value, schoolId, academicYear.getId(), normalize(classId), pageable)
                .map(student -> {
                    Enrollment enrollment = enrollmentRepo
                            .findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(), schoolId)
                            .orElse(null);
                    var feeDetail = getFeeDetailUsecase.execute(schoolId, student.getId());

                    return StudentMapper.toDTO(student, enrollment, feeDetail);
                });
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = (sortBy != null && SORTABLE_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    // Empty string, never null - see the repository's search query for why.
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
