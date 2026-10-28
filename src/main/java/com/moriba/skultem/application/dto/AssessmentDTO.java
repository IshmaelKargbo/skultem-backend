package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record AssessmentDTO(String id, String name, double weight, int position, List<Integer> terms, Instant createdAt, Instant updatedAt) {
}
