package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record StudentAssessmentDTO(String id, String name, List<AssessmentScoreDTO> scores,
        Integer totalScore, Integer passMark, Boolean passed,
        Instant createdAt, Instant updatedAt) {
}
