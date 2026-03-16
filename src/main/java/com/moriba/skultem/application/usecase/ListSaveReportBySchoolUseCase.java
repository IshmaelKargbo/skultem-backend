package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SaveReportDTO;
import com.moriba.skultem.application.mapper.SaveReportMapper;
import com.moriba.skultem.domain.repository.SaveReportRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSaveReportBySchoolUseCase {
    private final SaveReportRepository repo;

    public Page<SaveReportDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findAllBySchoolId(schoolId, pageable).map(SaveReportMapper::toDTO);
    }
}
