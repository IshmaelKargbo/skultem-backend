package com.moriba.skultem.application.dto;

import java.time.Instant;

public record SectionDTO(String id, String name, String description, int displayOrder, Instant createdAt, Instant updatedAt) {

}
