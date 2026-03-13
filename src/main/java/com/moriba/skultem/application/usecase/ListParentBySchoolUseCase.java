package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.mapper.ParentMapper;
import com.moriba.skultem.domain.repository.ParentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListParentBySchoolUseCase {
    private final ParentRepository repo;

    public Page<ParentDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return repo.findBySchool(schoolId, pageable).map(ParentMapper::toDTO);
    }
}
