package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportConfigDTO;
import com.moriba.skultem.application.mapper.ReportConfigViewMapper;
import com.moriba.skultem.domain.repository.ReportConfigRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListReportConfigBySchoolUseCase {
    private final ReportConfigRepository repo;

    public Page<ReportConfigDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findAllBySchoolId(schoolId, pageable).map(ReportConfigViewMapper::toDTO);
    }
}
