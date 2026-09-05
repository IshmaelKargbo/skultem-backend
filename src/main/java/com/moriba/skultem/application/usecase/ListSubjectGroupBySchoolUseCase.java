package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.application.mapper.SubjectGroupMapper;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSubjectGroupBySchoolUseCase {
    private final SubjectGroupRepository repo;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // g.<field>", so an unchecked client value would let someone probe/sort by arbitrary entity
    // fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "createdAt");

    public Page<SubjectGroupDTO> execute(String schoolId, int page, int size) {
        return execute(schoolId, page, size, null, null, null, null);
    }

    public Page<SubjectGroupDTO> execute(String schoolId, int page, int size, String classId, String query) {
        return execute(schoolId, page, size, classId, query, null, null);
    }

    public Page<SubjectGroupDTO> execute(String schoolId, int page, int size, String classId, String query,
            String sortBy, String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);
        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }

        boolean hasFilters = (classId != null && !classId.isBlank()) || (query != null && !query.isBlank());
        var groups = hasFilters
                ? repo.search(schoolId, normalize(classId), normalize(query), pageable)
                : repo.findBySchool(schoolId, pageable);

        return groups.map(SubjectGroupMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "name";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    // Empty string, never null - see the repository's search query for why.
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
