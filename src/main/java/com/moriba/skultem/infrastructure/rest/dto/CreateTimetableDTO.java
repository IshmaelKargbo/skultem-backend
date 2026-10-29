package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTimetableDTO(
        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Period is required")
        String period,

        @NotBlank(message = "Day is required")
        String day,

        @NotBlank(message = "color is required")
        String color,

        @NotBlank(message = "room is required")
        String room,
        // Set to edit that entry; leave out to add a subject (or another option) to the slot.
        String entry
) {}