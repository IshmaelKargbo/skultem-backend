package com.moriba.skultem.application.dto;

import java.time.Instant;

public record BehaviourDTO(String id, String student, String kind, String category, String note, Instant createdAt,
                Instant updatedAt) {
}
