package com.moriba.skultem.application.usecase;

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
public class GetActiveTermUseCase {

    private final TermRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public TermDTO execute(String schoolId) {
        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic not found"));
        var record = repo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId())
                .orElseThrow(() -> new NotFoundException("Active term not found"));
        return TermMapper.toDTO(record);
    }
}
