package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.application.mapper.SectionMapper;
import com.moriba.skultem.domain.repository.SectionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSectionBySchoolUseCase {

    private final SectionRepository repo;

    public Page<SectionDTO> execute(String school, int page, int size) {
        return execute(school, page, size, null);
    }

    public Page<SectionDTO> execute(String school, int page, int size, String query) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        boolean hasQuery = query != null && !query.isBlank();
        var sections = hasQuery ? repo.search(school, query.trim(), pageable) : repo.findBySchoolId(school, pageable);

        return sections.map(SectionMapper::toDTO);
    }
}
