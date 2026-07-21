package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RequestDemoDTO(UUID id, String name, String email, String school, String phone, String preferred,
        String priority, String message, Instant createdAt, Instant updatedAt) {
}
