package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class SubjectGroup extends AggregateRoot<String> {
    private String schoolId;
    private String name;
    private Level level;
    private Stream stream;
    private Clazz clazz;
    private int totalSelection;

    public SubjectGroup(String id, String schoolId, String name, Clazz clazz, Stream stream,
            int totalSelection, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.stream = stream;
        this.clazz = clazz;
        this.totalSelection = totalSelection;
        touch(updatedAt);
    }

    public static SubjectGroup create(String id, String schoolId, String name, Clazz clazz,
            Stream stream, int totalSelection) {
        Instant now = Instant.now();
        return new SubjectGroup(id, schoolId, name, clazz, stream, totalSelection, now, now);
    }
}
