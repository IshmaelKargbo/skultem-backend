package com.moriba.skultem.utils;

import java.math.BigDecimal;
import com.moriba.skultem.domain.model.ClassSession;

public class Formate {
    public static String formatMoney(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    public static String buildClassDisplayName(ClassSession clazz) {

        String baseName = clazz.getClazz().getName();

        if (clazz.getStream() == null) {
            return baseName + " (" + clazz.getSection().getName() + ")";
        }

        return baseName + " (" + clazz.getStream().getName() + " - " + clazz.getSection().getName() + ")";
    }
}