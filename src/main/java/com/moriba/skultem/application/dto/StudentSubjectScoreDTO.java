package com.moriba.skultem.application.dto;

// One subject's approved average score for one student - a row inside StudentAcademicPerformanceDTO.
public record StudentSubjectScoreDTO(
        String subjectId,
        String subjectName,
        double averageScore) {
}
