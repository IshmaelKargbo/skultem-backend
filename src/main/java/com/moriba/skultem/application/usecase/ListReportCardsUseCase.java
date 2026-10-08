package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

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
    private final SectionScopeService sectionScopeService;

    public Page<ReportCardSummaryDTO> execute(String schoolId, String classId, String termId, String search,
            String level, String sectionId, String streamId, int page, int size) {
        Pageable pageable = size > 0 ? PageRequest.of(Math.max(page - 1, 0), size) : Pageable.unpaged();

        // The picked level narrows what the caller's own section scope already allows - never widens it.
        var levels = sectionScopeService.levels().stream()
                .filter(e -> level == null || level.isBlank() || e.name().equalsIgnoreCase(level))
                .toList();

        return repo.search(schoolId, blankToNull(classId), blankToNull(termId), blankToNull(search),
                blankToNull(sectionId), blankToNull(streamId), levels, pageable)
                .map(ReportCardMapper::toSummaryDTO);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
