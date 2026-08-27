package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.domain.repository.ReportCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListReportCardsUseCase {
    private final ReportCardRepository repo;

    public Page<ReportCardSummaryDTO> execute(String schoolId, String classId, String termId, String search,
            int page, int size) {
        Pageable pageable = size > 0 ? PageRequest.of(Math.max(page - 1, 0), size) : Pageable.unpaged();

        return repo.search(schoolId, blankToNull(classId), blankToNull(termId), blankToNull(search), pageable)
                .map(ReportCardMapper::toSummaryDTO);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
