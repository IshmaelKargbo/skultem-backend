package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoomDTO(
        @NotBlank(message = "Room session is required")
        String name,

        @NotBlank(message = "Room number is required")
        String no,

        @NotBlank(message = "Room description is required")
        String description
) {}