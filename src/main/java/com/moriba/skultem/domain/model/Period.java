package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Period extends AggregateRoot<String> {
    private final String schoolId;
    private final ClassSession session;
    private final String name;
    private final Type type;
    private LocalTime startTime;
    private LocalTime endTime;

    public enum Type {
        PERIOD, BREAK, LUNCH,
        // The school (or the class) gathers - morning assembly, devotion. Nothing is taught in it, like a break.
        ASSEMBLY
    }

    /** How long a new assembly slot is; the time can be adjusted on the timetable afterwards. */
    public static final int DEFAULT_ASSEMBLY_MINUTES = 20;

    public Period(String id, String schoolId, ClassSession session, Type type, String name, LocalTime startTime, LocalTime endTime, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.type = type;
        this.schoolId = schoolId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.name = name;
        this.session = session;
        touch(updatedAt);
    }

    public boolean isBreak() {
        return this.type == Type.BREAK;
    }

    public boolean isLunch() {
        return this.type == Type.LUNCH;
    }

    public boolean isAssembly() {
        return this.type == Type.ASSEMBLY;
    }

    /** Break, lunch or assembly: a slot no lesson or Library period can go in. */
    public boolean isFixed() {
        return this.type != Type.PERIOD;
    }

    public static Period create(String id, String schoolId, ClassSession session, Type type, String name, LocalTime startTime, LocalTime endTime) {
        Instant now = Instant.now();
        return new Period(id, schoolId, session, type, name, startTime, endTime, now, now);
    }

    public void updateSchedule(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}