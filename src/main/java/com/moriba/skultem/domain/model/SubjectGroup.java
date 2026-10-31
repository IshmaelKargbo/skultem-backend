package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Level;

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

    // The class/stream a group belongs to is fixed - its subjects are already linked to the group
    // inside that class or stream - so only the name and how many subjects to pick can change.
    public void update(String name, int totalSelection) {
        this.name = name;
        this.totalSelection = totalSelection;
        touch(Instant.now());
    }

    public static SubjectGroup create(String id, String schoolId, String name, Clazz clazz,
            Stream stream, int totalSelection) {
        Instant now = Instant.now();
        return new SubjectGroup(id, schoolId, name, clazz, stream, totalSelection, now, now);
    }
}
