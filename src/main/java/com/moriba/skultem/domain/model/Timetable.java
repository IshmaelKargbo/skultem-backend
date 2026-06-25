package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Timetable extends AggregateRoot<String> {
    private final String schoolId;
    private final Period period;
    private final WorkingDay day;
    private TeacherSubject teacherSubject;
    private String color;
    private Room room;

    public Timetable(String id, String schoolId, Period period, TeacherSubject teacherSubject, WorkingDay day, Room room, String color, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.period = period;
        this.day = day;
        this.room = room;
        this.color = color;
        this.teacherSubject = teacherSubject;
        touch(updatedAt);
    }

    public static Timetable create(String id, String schoolId, Period period, TeacherSubject teacherSubject, WorkingDay day, Room room, String color) {
        Instant now = Instant.now();
        return new Timetable(id, schoolId, period, teacherSubject, day, room, color, now, now);
    }

    public void update(TeacherSubject subject, Room room, String color) {
        this.teacherSubject = subject;
        this.room = room;
        this.color = color;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}