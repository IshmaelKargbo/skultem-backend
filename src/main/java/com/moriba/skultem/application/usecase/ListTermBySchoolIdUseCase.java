package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListTermBySchoolIdUseCase {

    private final TermRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public Page<TermDTO> execute(String school, int page, int size) {
        var academicYear = academicYearRepo.findActiveBySchool(school).orElseThrow(() -> new NotFoundException("Active assessment not found"));
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findAllBySchoolIdAndAcademicYear(school, academicYear.getId(), pageable).map(TermMapper::toDTO);
    }
}
