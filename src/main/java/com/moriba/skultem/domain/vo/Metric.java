package com.moriba.skultem.domain.vo;

import java.util.List;
import java.util.Map;

public record Metric(String name, String aggregation, String field, Map<String, String> tags, Double value, List<Filter> filters) {
}
