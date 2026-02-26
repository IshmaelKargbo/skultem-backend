package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.AcademicYearMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ActiveAcademicYearUseCase {

    private final AcademicYearRepository repo;

    public AcademicYearDTO execute(String id) {
        var domain = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));
        
        repo.deactivateAllBySchool(domain.getSchoolId());
        domain.setActive(true);
        repo.save(domain);
        return AcademicYearMapper.toDTO(domain);
    }
}
