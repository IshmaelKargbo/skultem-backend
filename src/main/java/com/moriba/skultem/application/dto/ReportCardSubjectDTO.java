package com.moriba.skultem.application.dto;

import java.util.List;

public record ReportCardSubjectDTO(String subject, String teacher, Integer score, Integer weight,
        Integer weightScore, String grade, List<ReportCardAssessmentScoreDTO> assessments) {
}
