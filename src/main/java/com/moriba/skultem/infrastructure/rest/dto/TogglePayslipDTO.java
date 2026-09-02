package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record TogglePayslipDTO(

        @NotNull(message = "Included is required")
        Boolean included

) {
}
