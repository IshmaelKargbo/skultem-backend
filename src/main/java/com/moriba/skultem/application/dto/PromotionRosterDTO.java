package com.moriba.skultem.application.dto;

import java.util.List;

public record PromotionRosterDTO(
        String sessionName,
        String targetClassName,
        boolean eligible,
        String ineligibleReason,
        boolean requiresStreamSelection,
        List<StreamDTO> availableStreams,
        List<RosterStudentDTO> students) {
}
