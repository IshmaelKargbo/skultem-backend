package com.moriba.skultem.application.dto;

// One assessment's contribution to a subject's term total (Test 1, Test 2,
// Exam, ...) - kept alongside the subject's combined score/grade so the
// report card can show the per-assessment breakdown, not just the total.
public record ReportCardAssessmentScoreDTO(String name, Integer score, Integer weight, Integer level) {
}
