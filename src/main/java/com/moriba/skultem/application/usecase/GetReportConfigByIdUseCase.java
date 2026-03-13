package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportConfigDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ReportConfigViewMapper;
import com.moriba.skultem.domain.repository.ReportConfigRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetReportConfigByIdUseCase {
    private final ReportConfigRepository repo;

    public ReportConfigDTO execute(String schoolId, String id) {
        var record = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Report config not found"));
        return ReportConfigViewMapper.toDTO(record);
    }
}
