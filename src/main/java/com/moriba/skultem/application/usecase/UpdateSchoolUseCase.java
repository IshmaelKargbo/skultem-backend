package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.model.School.GenderComposition;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Address;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSchoolUseCase {
    private final SchoolRepository repo;

    public SchoolDTO execute(String schoolId, String name, String domain, Address address,
            Double attendanceThreshold, String genderComposition) {
        var school = repo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));
        school.update(name, domain, address, attendanceThreshold, parseGenderComposition(genderComposition));
        repo.save(school);
        return SchoolMapper.toDTO(school);
    }

    private GenderComposition parseGenderComposition(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GenderComposition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuleException("Gender composition must be one of BOYS, GIRLS or MIXED");
        }
    }
}
