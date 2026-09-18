package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.service.PerformanceTrendCalculator.Trend;

// Result of GetStudentPerformanceTrendUseCase - trend is INSUFFICIENT_DATA rather than a guess
// when fewer than two approved assessments exist yet, per the "don't make unsupported claims"
// requirement.
public record StudentPerformanceTrendDTO(
        Trend trend,
        List<PerformanceTrendPointDTO> points) {
}
