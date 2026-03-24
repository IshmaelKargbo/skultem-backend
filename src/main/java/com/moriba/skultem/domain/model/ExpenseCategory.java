package com.moriba.skultem.domain.model;

import java.time.Instant;
import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class ExpenseCategory extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private String description;

    public ExpenseCategory(String id, String schoolId, String name, String description, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.description = description;
        touch(updatedAt);
    }

    public static ExpenseCategory create(String id, String schoolId, String name, String description) {
        Instant now = Instant.now();
        return new ExpenseCategory(id, schoolId, name, description, now, now);
    }
}
