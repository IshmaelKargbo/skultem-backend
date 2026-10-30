package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.NotBlank;

public record CreateTimetableDTO(
        // Required for a lesson; left out for a Library period.
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
        String entry,

        // SUBJECT (default) or LIBRARY.
        @Pattern(regexp = "SUBJECT|LIBRARY", message = "Kind must be SUBJECT or LIBRARY")
        String kind
) {}