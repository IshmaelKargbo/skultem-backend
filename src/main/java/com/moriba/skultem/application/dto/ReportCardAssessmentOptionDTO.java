package com.moriba.skultem.application.dto;

// One assessment of a class's template (First Test, Second Test, Exam, ...) that a report card can be limited to.
// terms: the term numbers the test runs in; empty = every term.
public record ReportCardAssessmentOptionDTO(String id, String name, Integer weight, int position, java.util.List<Integer> terms) {
}
