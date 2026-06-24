package com.moriba.skultem.application.dto;

import java.time.Instant;

public record RoomDTO(String id, String name, String no, String description, Instant createdAt, Instant updatedAt) {
}
