package com.moriba.skultem.application.dto;

import java.util.List;

public record ReportCardSubjectDTO(String subject, String teacher, Integer score, Integer weight,
        Integer weightScore, String grade, List<ReportCardAssessmentScoreDTO> assessments,
        // Whole-year cards only: the subject's score in each term (score above is the final, year score).
        List<ReportCardTermScoreDTO> termScores) {
}
