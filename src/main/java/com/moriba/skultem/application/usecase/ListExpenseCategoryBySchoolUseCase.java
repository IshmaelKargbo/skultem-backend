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
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findBySchool(schoolId, pageable).map(ExpenseCategoryMapper::toDTO);
    }
}
