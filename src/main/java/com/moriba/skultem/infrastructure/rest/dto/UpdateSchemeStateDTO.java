package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSchemeStateDTO(

        @NotBlank(message = "State is required")
        @Pattern(regexp = "DRAFT|PUBLISH", message = "State must be one of DRAFT, PUBLISH")
        String state

) {}
