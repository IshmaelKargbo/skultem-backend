package com.moriba.skultem.application.dto;

import java.time.Instant;

public record AssessmentDTO(String id, String name, double weight, int position, Instant createdAt, Instant updatedAt) {
}
