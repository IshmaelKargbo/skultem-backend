package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Clazz extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private Level level;
    private Boolean terminal;
    private int displayOrder;
    private Clazz nextClass;
    private Status status;

    public enum Status {
        ACTIVE,
        DELETED
    }

    public Clazz(String id, String school, String name, Level level, int displayOrder, Clazz nextClass,
            Boolean terminal, Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = school;
        this.level = level;
        this.displayOrder = displayOrder;
        this.nextClass = nextClass;
        this.terminal = terminal;
        this.status = status;
        touch(updatedAt);
    }

    public static Clazz create(String id, String school, String name, Level level, int displayOrder) {
        Instant now = Instant.now();
        return new Clazz(id, school, name, level, displayOrder, null, false, Status.ACTIVE, now, now);
    }

    public void setTerminal(boolean state) {
        this.terminal = state;
        touch(Instant.now());
    }

    public void setNextClass(Clazz nextClass) {
        this.nextClass = nextClass;
        touch(Instant.now());
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }
}
