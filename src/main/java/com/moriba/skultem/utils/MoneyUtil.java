package com.moriba.skultem.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyUtil {

    private static final Locale LOCALE = Locale.US;

    public static String format(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(LOCALE);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);
        return "NLE " + formatter.format(amount);
    }
}