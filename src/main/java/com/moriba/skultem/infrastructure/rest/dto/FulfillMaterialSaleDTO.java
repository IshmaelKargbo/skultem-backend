package com.moriba.skultem.infrastructure.rest.dto;

import org.hibernate.validator.constraints.Length;

public record FulfillMaterialSaleDTO(
        @Length(max = 255, message = "Note must not exceed 255 characters")
        String note) {
}
