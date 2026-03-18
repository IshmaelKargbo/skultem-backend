package com.moriba.skultem.domain.vo;

import java.util.List;
import java.util.Map;

public record Chart(String chartType, String title, List<String> labels, List<Map<String, Object>> datasets) {

}
