package com.moriba.skultem.application.dto;

public record PromotionRequestItemDTO(
        String studentId,
        String enrollmentId,
        String studentName,
        String admissionNumber,
        String outcome,
        String remark,
        String targetStreamId,
        String targetStreamName,
        Double average) {
}
