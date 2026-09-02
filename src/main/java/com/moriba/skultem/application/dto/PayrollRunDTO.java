package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.PayrollRun.Status;

public record PayrollRunDTO(
        String id,
        String schoolId,
        String period,
        LocalDate payDate,
        Status status,
        Instant createdAt,
        Instant updatedAt) {
}
