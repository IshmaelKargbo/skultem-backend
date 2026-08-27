package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record PromotionRequestDTO(
        String id,
        String sessionId,
        String sessionName,
        String classMasterName,
        String academicYearName,
        String targetClassName,
        String status,
        String teacherNote,
        String returnReason,
        String approvalNote,
        int promoteCount,
        int repeatCount,
        int promotedCount,
        int repeatedCount,
        Instant submittedAt,
        Instant executedAt,
        List<PromotionRequestItemDTO> items) {
}
