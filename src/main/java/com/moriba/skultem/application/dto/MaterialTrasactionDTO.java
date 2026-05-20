package com.moriba.skultem.application.dto;

import java.time.Instant;

public record MaterialTrasactionDTO(String id, MaterialDTO material, int qty, String note, Instant createdAt, Instant updatedAt) {
}
