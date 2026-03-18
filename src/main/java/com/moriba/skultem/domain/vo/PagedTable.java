package com.moriba.skultem.domain.vo;

import java.util.List;
import java.util.Map;

public record PagedTable(
        List<Map<String, Object>> data,
        Map<String, Object> meta) {
}