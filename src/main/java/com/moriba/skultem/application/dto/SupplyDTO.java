package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.Supply.Status;

public record SupplyDTO(String id, StudentDTO student, MaterialDTO material, int qty,
        int collectedQty, Instant collectedOn, Status status, Instant createdAt, Instant updatedAt) {
}
