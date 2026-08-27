package com.moriba.skultem.application.dto;

import java.util.List;

public record GenerateReportCardsResultDTO(int generated, long passed, long failed, double classAverage,
        List<ReportCardSummaryDTO> reportCards) {
}
