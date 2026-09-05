package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.mapper.SubjectMapper;
import com.moriba.skultem.domain.repository.SubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSubjectBySchoolUseCase {
    private final SubjectRepository repo;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // s.<field>", so an unchecked client value would let someone probe/sort by arbitrary entity
    // fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "code", "createdAt");

    public Page<SubjectDTO> execute(String schoolId, int page, int size) {
        return execute(schoolId, page, size, null, null, null);
    }

    public Page<SubjectDTO> execute(String schoolId, int page, int size, String query) {
        return execute(schoolId, page, size, query, null, null);
    }

    public Page<SubjectDTO> execute(String schoolId, int page, int size, String query, String sortBy,
            String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);
        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }

        boolean hasQuery = query != null && !query.isBlank();
        var subjects = hasQuery ? repo.search(schoolId, query.trim(), pageable) : repo.findBySchool(schoolId, pageable);

        return subjects.map(SubjectMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "name";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
