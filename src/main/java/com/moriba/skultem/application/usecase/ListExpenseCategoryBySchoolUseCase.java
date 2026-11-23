package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExpenseCategoryDTO;
import com.moriba.skultem.application.mapper.ExpenseCategoryMapper;
import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListExpenseCategoryBySchoolUseCase {
    private final ExpenseCategoryRepository repo;

    public Page<ExpenseCategoryDTO> execute(String schoolId, int page, int size) {
        return execute(schoolId, page, size, null);
    }

    // query: words to look for (blank = everything).
    public Page<ExpenseCategoryDTO> execute(String schoolId, int page, int size, String query) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        String q = query == null ? "" : query.trim();
        var found = q.isEmpty() ? repo.findBySchool(schoolId, pageable) : repo.search(schoolId, q, pageable);
        return found.map(ExpenseCategoryMapper::toDTO);
    }
}
