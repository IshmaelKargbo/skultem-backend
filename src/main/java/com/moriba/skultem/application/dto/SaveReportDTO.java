package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.vo.Filter;

public record SaveReportDTO(
        String id,
        String name,
        String entity,
        List<Filter> filters,
        Instant createdAt,
        Instant updatedAt) {
}
