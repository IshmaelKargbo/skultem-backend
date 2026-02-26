package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListTermByAcademicYearIdUseCase {

    private final TermRepository repo;

    public Page<TermDTO> execute(String school, String academicYearId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findAllBySchoolIdAndAcademicYear(school, academicYearId, pageable).map(TermMapper::toDTO);
    }
}
