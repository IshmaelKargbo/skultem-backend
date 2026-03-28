package com.moriba.skultem.application.dto;

public record LeaderBoardAssessmentDTO(
        String assessmentId,
        String assessmentName,
        Integer score,
        Integer weight,
        Integer position,
        Integer weightedScore
) {
}