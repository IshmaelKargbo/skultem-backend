package com.moriba.skultem.application.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.moriba.skultem.application.dto.PaymentMethodAmountDTO;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;

// Turns a [method, amount, count] projection (PaymentJpaRepository#sumSchoolPaymentsByMethodAndDateRange)
// into the sorted, percentage-of-total rows both the Daily Collection and Payment Method Summary
// reports show, so the two can never compute "percentage of total" differently.
public class PaymentMethodRowMapper {

    private PaymentMethodRowMapper() {
    }

    public record Totals(BigDecimal amount, int count) {
    }

    public static Totals totals(List<Object[]> rows) {
        BigDecimal amount = BigDecimal.ZERO;
        int count = 0;
        for (Object[] row : rows) {
            amount = amount.add((BigDecimal) row[1]);
            count += ((Number) row[2]).intValue();
        }
        return new Totals(amount, count);
    }

    public static List<PaymentMethodAmountDTO> toRows(List<Object[]> rows, BigDecimal grandTotal) {
        List<PaymentMethodAmountDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            int count = ((Number) row[2]).intValue();
            double percentage = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(grandTotal, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue()
                    : 0.0;
            result.add(new PaymentMethodAmountDTO(method.name(), amount, count, percentage));
        }
        result.sort(Comparator.comparing(PaymentMethodAmountDTO::amount).reversed());
        return result;
    }
}
