package com.moriba.skultem.application.dto;

import java.util.List;

// score/weightScore are whole numbers or one decimal place, depending on the school's "Round assessment scores"
// setting (older cards stored whole numbers, which still read fine as decimals).
public record ReportCardSubjectDTO(String subject, String teacher, Double score, Integer weight,
        Double weightScore, String grade, List<ReportCardAssessmentScoreDTO> assessments,
        // Whole-year cards only: the subject's score in each term (score above is the final, year score).
        List<ReportCardTermScoreDTO> termScores) {
}
