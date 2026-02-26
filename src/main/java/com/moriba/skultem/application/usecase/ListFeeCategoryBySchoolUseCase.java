package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeCategoryDTO;
import com.moriba.skultem.application.mapper.FeeCategoryMapper;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListFeeCategoryBySchoolUseCase {
    private final FeeCategoryRepository repo;

    public Page<FeeCategoryDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findBySchool(schoolId, pageable).map(FeeCategoryMapper::toDTO);
    }
}
