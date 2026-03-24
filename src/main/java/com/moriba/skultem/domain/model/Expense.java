package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class Expense extends AggregateRoot<String> {
    private String schoolId;
    private String title;
    private ExpenseCategory category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;

    public Expense(String id, String schoolId, String title, BigDecimal amount, ExpenseCategory category, String description, LocalDate expenseDate, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
        touch(updatedAt);
    }

    public static Expense create(String id, String schoolId, String title, BigDecimal amount, ExpenseCategory category, String description, LocalDate expenseDate) {
        Instant now = Instant.now();
        return new Expense(id, schoolId, title, amount, category, description, expenseDate, now, now);
    }
}
