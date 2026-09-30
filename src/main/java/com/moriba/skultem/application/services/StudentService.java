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
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Gender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final GetFeeDetailUsecase getFeeDetailUsecase;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final SectionScopeService sectionScopeService;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // s.<field>", so an unchecked client value would let someone probe/sort by arbitrary entity
    // fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("givenNames", "familyName", "admissionNumber",
            "createdAt");

    public Page<StudentDTO> search(String value, int page, int size, String schoolId, String academicYearId) {
        return search(value, page, size, schoolId, academicYearId, null, null, null, null);
    }

    public Page<StudentDTO> search(String value, int page, int size, String schoolId, String academicYearId,
            String classId, String sortBy, String direction, Gender gender) {
        Pageable pageable = PageableMapper.toPage(page, size, resolveSort(sortBy, direction));

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        return studentRepo.search(value, schoolId, academicYear.getId(), Student.Status.ACTIVE, normalize(classId),
                gender == null ? "" : gender.name(), sectionScopeService.levels(), pageable)
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
