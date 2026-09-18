package com.moriba.skultem.application.dto;

// One band of a subject's grade distribution, per the school's own configured grading scale
// (School.gradingScale) - never an invented scale.
public record GradeCountDTO(
        String grade,
        long count) {
}
