package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

public record SchemeProgressDTO(String id, int totalWeeks, int completed, int remaining, BigDecimal coverage) {
}
