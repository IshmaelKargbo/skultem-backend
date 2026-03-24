package com.moriba.skultem.application.dto;

import java.util.List;


import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.Metric;

public record Widget(String entity, List<Filter> filters, List<Metric> metrics, String chartType, String title) {
    
}