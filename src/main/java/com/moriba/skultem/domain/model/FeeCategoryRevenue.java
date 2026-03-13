package com.moriba.skultem.domain.model;

import java.math.BigDecimal;

public class FeeCategoryRevenue {

    private String category;
    private BigDecimal amount;

    public FeeCategoryRevenue(String category, BigDecimal amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}