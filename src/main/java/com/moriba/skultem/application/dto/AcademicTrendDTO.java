package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.service.PerformanceTrendCalculator.Trend;

// Class/school-level (not per-student) counterpart to StudentPerformanceTrendDTO - the Academic
// Trends section. trend is INSUFFICIENT_DATA rather than a guess when fewer than two approved
// assessment positions exist yet.
public record AcademicTrendDTO(
        Trend trend,
        List<PerformanceTrendPointDTO> points) {
}
