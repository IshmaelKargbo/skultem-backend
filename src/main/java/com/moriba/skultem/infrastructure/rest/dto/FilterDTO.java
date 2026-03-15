package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.validation.constraints.NotBlank;

public record FilterDTO(
                @NotBlank(message = "Field is required") String field,

                @NotBlank(message = "Operator is required") FilterOperator operator,

                @NotBlank(message = "Type is required") String type,

                String value,

                String valueTo,

                List<String> values) {
}