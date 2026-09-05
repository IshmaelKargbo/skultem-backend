package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.mapper.TeacherSubjectMapper;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListTeacherSubjectBySchoolUseCase {

    private final TeacherSubjectRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // ts.<field>"/"order by t.<field>", so an unchecked client value would let someone probe/sort
    // by arbitrary entity fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("assignedAt", "teacher.user.givenName",
            "subject.name");

    public Page<TeacherSubjectDTO> execute(String school, String academicYearId, int page, int size) {
        return execute(school, academicYearId, null, null, page, size, null, null);
    }

    public Page<TeacherSubjectDTO> execute(String school, String academicYearId, String classId, String query,
            int page, int size) {
        return execute(school, academicYearId, classId, query, page, size, null, null);
    }

    public Page<TeacherSubjectDTO> execute(String school, String academicYearId, String classId, String query,
            int page, int size, String sortBy, String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);

        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }

        var academicYear = resolveAcademicYearUseCase.execute(school, academicYearId);

        boolean hasFilters = (classId != null && !classId.isBlank()) || (query != null && !query.isBlank());
        var assignments = hasFilters
                ? repo.search(school, academicYear.getId(), normalize(classId), normalize(query), pageable)
                : repo.findAllBySchoolIdAndAcademicYearId(school, academicYear.getId(), pageable);

        return assignments.map(TeacherSubjectMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "assignedAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    // Empty string, never null - see the repository's search query for why.
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
