package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteParentDTO(@NotBlank(message = "Type the guardian's name to confirm") String confirmation) {
}
