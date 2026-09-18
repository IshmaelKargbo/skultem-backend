package com.moriba.skultem.application.dto;

// One point in a student's performance trend series - label is the assessment's name (e.g. "CA1",
// "Mid-Term"), not a date, since trend is ordered by assessment position within the term.
public record PerformanceTrendPointDTO(
        String label,
        double score) {
}
