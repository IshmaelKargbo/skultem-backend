package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.repository.ClassRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassBySchoolUseCase {

    private final ClassRepository repo;
    private final SectionScopeService sectionScopeService;

    public Page<ClassDTO> execute(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findBySchool(school, sectionScopeService.levels(), pageable).map(ClassMapper::toDTO);
    }
}
