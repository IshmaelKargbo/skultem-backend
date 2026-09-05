package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SalaryTemplateDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SalaryTemplateMapper;
import com.moriba.skultem.domain.repository.SalaryTemplateRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetSalaryTemplateUseCase {
    private final SalaryTemplateRepository repo;

    public SalaryTemplateDTO execute(String schoolId, String templateId) {
        var template = repo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Salary template not found"));

        return SalaryTemplateMapper.toDTO(template);
    }
}
