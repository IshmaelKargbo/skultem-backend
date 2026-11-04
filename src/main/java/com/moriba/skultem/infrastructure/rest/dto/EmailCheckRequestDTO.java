package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Size;

public record EmailCheckRequestDTO(@Size(max = 320) String email) {
}
