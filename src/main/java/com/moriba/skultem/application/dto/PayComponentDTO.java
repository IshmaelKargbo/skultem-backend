package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

// One named allowance/deduction line, with its resolved amount already computed (percentage lines
// resolved against whatever basicSalary they belong to) so the frontend never has to duplicate
// that math just to show a total.
public record PayComponentDTO(
        String name,
        String type,
        BigDecimal value,
        BigDecimal resolvedAmount) {
}
