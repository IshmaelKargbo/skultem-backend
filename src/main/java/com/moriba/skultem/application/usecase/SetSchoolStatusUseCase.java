package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SetSchoolStatusUseCase {
    private final SchoolRepository repo;

    public SchoolDTO execute(String schoolId, String status) {
        var school = repo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));
        school.setStatus(School.Status.valueOf(status));
        repo.save(school);
        return SchoolMapper.toDTO(school);
    }
}
