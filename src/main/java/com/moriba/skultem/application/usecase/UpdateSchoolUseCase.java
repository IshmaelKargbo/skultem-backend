package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Address;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSchoolUseCase {
    private final SchoolRepository repo;

    public SchoolDTO execute(String schoolId, String name, String domain, Address address) {
        var school = repo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));
        school.update(name, domain, address);
        repo.save(school);
        return SchoolMapper.toDTO(school);
    }
}
