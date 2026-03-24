package com.moriba.skultem.application.dto;

import java.util.List;

public record ChartData(
        String chartType,
        String title,
        List<String> labels,
        List<Dataset> datasets
) {}