package com.moriba.skultem.application.dto;

import java.util.List;

public record LeaderBoardDTO(
        String id,
        int rank,
        String name,
        Integer score,
        Integer weight,
        String trend,
        List<LeaderBoardAssessmentDTO> assessments
) {
}