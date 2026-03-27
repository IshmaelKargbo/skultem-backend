package com.moriba.skultem.application.dto;

import java.util.List;

public record BreakdownDTO(String id, String name, Integer score, String grade, String trend,
        List<AssessmentScoreDTO> scores) {
}
