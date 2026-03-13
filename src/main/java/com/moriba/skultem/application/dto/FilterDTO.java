package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.FilterOperator;

public record FilterDTO(String field, FilterOperator operator, String value, String from, String to,
        List<String> values) {
}