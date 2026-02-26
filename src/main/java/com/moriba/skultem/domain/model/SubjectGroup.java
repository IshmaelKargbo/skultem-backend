package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class SubjectGroup extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private Stream stream;
    private Clazz clazz;
    private Boolean required;
    private int minSelection;
    private int maxSelection;
    private int displayOrder;
    private Level level;

    public SubjectGroup(String id, String schoolId, String name, Level level, Clazz clazz, Stream stream,
            Boolean required, int minSelection, int maxSelection, int displayOrder, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.level = level;
        this.schoolId = schoolId;
        this.stream = stream;
        this.clazz = clazz;
        this.required = required;
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
        this.displayOrder = displayOrder;
        touch(updatedAt);
    }

    public static SubjectGroup create(String id, String schoolId, String name, Level level, Clazz clazz,
            Stream stream, Boolean required, int minSelection, int maxSelection, int displayOrder) {
        Instant now = Instant.now();
        return new SubjectGroup(id, schoolId, name, level, clazz, stream, required, minSelection, maxSelection,
                displayOrder, now, now);
    }
}
