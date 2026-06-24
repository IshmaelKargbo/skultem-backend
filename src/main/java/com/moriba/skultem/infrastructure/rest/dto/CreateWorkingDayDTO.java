package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateWorkingDayDTO(
        @NotEmpty(message = "At least one working day is required")
        List<@Valid WorkingDayItemDTO> days

) {
    public record WorkingDayItemDTO(
            @Pattern(
                    regexp = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY",
                    message = "Working day is invalid"
            ) String day,
            boolean state
    ) {
    }
}