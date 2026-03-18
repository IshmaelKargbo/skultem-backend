package com.moriba.skultem.domain.vo;

import java.util.List;

public record ChartDataset(
        String label,
        List<Double> data
) {
}