package com.moriba.skultem.application.dto;

public record AssessmentCycleAdvanceDTO(
        String termId,
        int currentPosition,
        Integer nextPosition,
        int totalPositions,
        boolean advanced,
        boolean completed,
        String message) {
}
