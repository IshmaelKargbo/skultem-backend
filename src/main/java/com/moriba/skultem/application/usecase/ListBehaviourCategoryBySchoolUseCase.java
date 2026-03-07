package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourCategoryDTO;
import com.moriba.skultem.application.mapper.BehaviourCategoryMapper;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListBehaviourCategoryBySchoolUseCase {
    private final BehaviourCategoryRepository repo;

    public Page<BehaviourCategoryDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findAllSchoolId(schoolId, pageable).map(BehaviourCategoryMapper::toDTO);
    }
}
