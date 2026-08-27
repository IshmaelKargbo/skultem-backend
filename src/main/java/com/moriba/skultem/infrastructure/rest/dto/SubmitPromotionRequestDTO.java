package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SubmitPromotionRequestDTO(
        String note,
        @NotEmpty(message = "At least one student is required") @Valid List<PromotionItemInputDTO> items) {
}
