package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateClassTerminalDTO(
        @NotNull(message = "Terminal is required") Boolean terminal) {
}
