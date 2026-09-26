package com.moriba.skultem.application.dto;

public record AssessmentCycleAdvanceDTO(
        String termId,
        int currentPosition,
        Integer nextPosition,
        int totalPositions,
        boolean advanced,
        boolean completed,
        String message,
        // The section that was moved (null in a school without sections) and whether that section has now closed
        // its last assessment of the term - while the term itself stays open for the sections still running.
        String sectionId,
        String sectionName,
        boolean sectionCompleted) {
}
