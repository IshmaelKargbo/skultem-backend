package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.SchoolRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSchoolUseCase {

    private final SchoolRepository repo;

    public Page<SchoolDTO> execute(int page, int size, String query) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        boolean hasQuery = query != null && !query.isBlank();
        var schools = hasQuery ? repo.search(query.trim(), pageable) : repo.findAll(pageable);

        return schools.map(SchoolMapper::toDTO);
    }
}
