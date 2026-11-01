package com.moriba.skultem.domain.vo;

import java.math.BigDecimal;

// One allowance or deduction line on a payslip, already resolved to an amount ("Housing
// Allowance", 300000) - a frozen copy, unlike PayComponent which may be a percentage of basic.
public record PayslipLine(String name, BigDecimal amount) {
}
