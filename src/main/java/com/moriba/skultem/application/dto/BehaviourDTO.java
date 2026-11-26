package com.moriba.skultem.application.dto;

import java.time.Instant;

public record BehaviourDTO(String id, String student, String kind, String category, String note, Instant createdAt,
                Instant updatedAt,
                // Where the student was when it was recorded, e.g. "JSS 1 · 2025/2026" (blank if unknown).
                String classLabel) {
}
