package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SalaryTemplateDTO;
import com.moriba.skultem.application.mapper.SalaryTemplateMapper;
import com.moriba.skultem.domain.repository.SalaryTemplateRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSalaryTemplateBySchoolUseCase {
    private final SalaryTemplateRepository repo;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - see ListSubjectBySchoolUseCase
    // for why.
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "basicSalary", "createdAt");

    public Page<SalaryTemplateDTO> execute(String schoolId, int page, int size) {
        return execute(schoolId, page, size, null, null, null);
    }

    public Page<SalaryTemplateDTO> execute(String schoolId, int page, int size, String query, String sortBy,
            String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);
        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }

        boolean hasQuery = query != null && !query.isBlank();
        var templates = hasQuery
                ? repo.search(schoolId, query.trim(), pageable)
                : repo.findAllBySchoolId(schoolId, pageable);

        return templates.map(SalaryTemplateMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        // Set.of(...).contains(null) throws NPE, not just "false" like a HashSet would - sortBy is
        // null whenever a caller doesn't pass it at all, so that null check must come first.
        String field = (sortBy != null && SORTABLE_FIELDS.contains(sortBy)) ? sortBy : "name";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
