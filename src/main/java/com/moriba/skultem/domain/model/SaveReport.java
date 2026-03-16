package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Filter;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class SaveReport extends AggregateRoot<String> {
    private final String schoolId;
    private String name;
    private List<Filter> filters;
    private String entity;

    public SaveReport(
            String id,
            String schoolId,
            String name,
            List<Filter> filters,
            String entity,
            Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.entity = entity;
        this.filters = filters;
        touch(updatedAt);
    }

    public static SaveReport create(
            String id,
            String schoolId,
            String name,
            List<Filter> filters,
            String entity) {
        Instant now = Instant.now();
        return new SaveReport(
                id,
                schoolId,
                name,
                filters,
                entity,
                now,
                now);
    }

    public void updateFilter(String name, List<Filter> filters) {
        this.name = name;
        this.filters = filters;
        this.touch(Instant.now());
    }
}
