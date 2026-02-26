package com.moriba.skultem.application.dto;

import java.time.Instant;

public record SubjectDTO(String id, String name, String code, String description, Instant createdAt, Instant updatedAt) {
}
