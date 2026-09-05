package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PlatformFeeSettingDTO(String schoolId, BigDecimal amount, Instant updatedAt) {
}
