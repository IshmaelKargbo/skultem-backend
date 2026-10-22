package com.moriba.skultem.application.dto;

import java.util.List;

// One subject on a student's performance page. score is the weighted percentage over the assessments approved so
// far (null until one is), so a subject with one of three tests marked reads as that test's result, not a third of it.
public record BreakdownDTO(String id, String name, Integer score, String grade, String trend,
        List<AssessmentScoreDTO> scores, Boolean passed,
        // The template's pass mark the score is held against.
        Integer passMark,
        // Assessments approved so far, out of all this subject has this term.
        Integer assessed, Integer total) {
}
