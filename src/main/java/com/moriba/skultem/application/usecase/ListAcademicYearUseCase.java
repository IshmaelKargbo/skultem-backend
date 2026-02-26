package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.mapper.AcademicYearMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListAcademicYearUseCase {

    private final AcademicYearRepository repo;

    public Page<AcademicYearDTO> execute(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findAllBySchool(school, pageable).map(AcademicYearMapper::toDTO);
    }
}
