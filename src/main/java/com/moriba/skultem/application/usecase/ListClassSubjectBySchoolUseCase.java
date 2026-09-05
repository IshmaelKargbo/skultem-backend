package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSubjectDTO;
import com.moriba.skultem.application.mapper.ClassSubjectMapper;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassSubjectBySchoolUseCase {

    private final ClassSubjectRepository repo;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // cs.<field>", so an unchecked client value would let someone probe/sort by arbitrary entity
    // fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("clazz.levelOrder", "subject.name", "createdAt");

    public Page<ClassSubjectDTO> execute(String school, int page, int size) {
        return execute(school, page, size, null, null, null, null, null);
    }

    public Page<ClassSubjectDTO> execute(String school, int page, int size, String classId, Boolean mandatory,
            String query) {
        return execute(school, page, size, classId, mandatory, query, null, null);
    }

    public Page<ClassSubjectDTO> execute(String school, int page, int size, String classId, Boolean mandatory,
            String query, String sortBy, String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);

        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }

        boolean hasFilters = (classId != null && !classId.isBlank()) || mandatory != null
                || (query != null && !query.isBlank());
        var subjects = hasFilters
                ? repo.search(school, normalize(classId), mandatory, normalize(query), pageable)
                : repo.findBySchool(school, pageable);

        return subjects.map(ClassSubjectMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "clazz.levelOrder";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    // Empty string, never null - see the repository's search query for why.
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
