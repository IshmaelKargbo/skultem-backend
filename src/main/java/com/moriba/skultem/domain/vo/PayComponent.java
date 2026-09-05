package com.moriba.skultem.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

// One named allowance or deduction line - "Housing Allowance: 20% (of basic)" or "Transport: 50
// (flat)" - used both inside a SalaryTemplate (a reusable starting point) and a SalaryStructure (a
// specific teacher's actual pay, whether built from a template or entirely by hand). No identity
// of its own - the whole list is always replaced wholesale whenever a template or structure is
// saved (see CreateSalaryTemplateUseCase / SetSalaryStructureUseCase), so there's nothing to key
// an individual line on across edits.
public record PayComponent(String name, PayComponentType type, BigDecimal value) {

    // Resolves this line to an actual amount given the basic salary it's percentage-of, if it is
    // one. A FIXED line ignores basicSalary entirely.
    public BigDecimal resolve(BigDecimal basicSalary) {
        if (type == PayComponentType.PERCENTAGE) {
            BigDecimal base = basicSalary == null ? BigDecimal.ZERO : basicSalary;
            return base.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        return value;
    }
}
