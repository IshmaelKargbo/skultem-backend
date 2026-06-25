package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record EditRoomDTO(
        @NotBlank(message = "Room id is required")
        String id,

        @NotBlank(message = "Room name is required")
        String name,

        @NotBlank(message = "Room number is required")
        String no,

        @NotBlank(message = "Room description is required")
        String description
) {}