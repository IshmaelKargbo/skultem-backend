package com.moriba.skultem.application.dto;

// One assessment of a class's template (First Test, Second Test, Exam, ...) that a report card can be limited to.
public record ReportCardAssessmentOptionDTO(String id, String name, Integer weight, int position) {
}
