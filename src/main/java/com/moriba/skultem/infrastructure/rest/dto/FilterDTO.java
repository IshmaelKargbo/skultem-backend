package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.validation.constraints.NotBlank;

public record FilterDTO(
                @NotBlank(message = "Field is required") String field,

                FilterOperator operator,

                // single value operators (=, !=, contains)
                String value,

                // range operators (between dates or numbers)
                String from,
                String to,

                // multi-select operators (IN, NOT_IN)
                List<String> values) {
}