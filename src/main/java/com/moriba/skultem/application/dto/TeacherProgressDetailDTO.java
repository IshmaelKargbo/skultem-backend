package com.moriba.skultem.application.dto;

import java.util.List;

public record TeacherProgressDetailDTO(
        String id,
        String name,
        int subjects,
        int classes,
        long lessonNotes,
        int completedWeeks,
        int totalWeeks,
        int coverage,
        String status,
        List<SubjectCoverageDTO> subjectCoverage) {
}
