package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

/**
 * One entry in a class's weekly timetable: a period on a working day. Either a subject lesson (SUBJECT - the
 * teacher assigned to that subject teaches it) or a Library period (LIBRARY - the librarian runs it, so there is
 * no subject or teacher). Library periods are the scheduled sessions Library Attendance takes a register for.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class Timetable extends AggregateRoot<String> {

    public enum Kind {
        SUBJECT, LIBRARY
    }

    public static final String LIBRARY_LABEL = "Library";

    private final String schoolId;
    private final Period period;
    private final WorkingDay day;
    private Kind kind;
    // Null for a Library period.
    private TeacherSubject teacherSubject;
    private String color;
    private Room room;

    public Timetable(String id, String schoolId, Period period, Kind kind, TeacherSubject teacherSubject,
            WorkingDay day, Room room, String color, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.period = period;
        this.day = day;
        this.room = room;
        this.color = color;
        this.kind = kind == null ? Kind.SUBJECT : kind;
        this.teacherSubject = teacherSubject;
        touch(updatedAt);
    }

    public static Timetable create(String id, String schoolId, Period period, TeacherSubject teacherSubject, WorkingDay day, Room room, String color) {
        if (teacherSubject == null) {
            throw new IllegalArgumentException("Choose a subject");
        }
        Instant now = Instant.now();
        return new Timetable(id, schoolId, period, Kind.SUBJECT, teacherSubject, day, room, color, now, now);
    }

    public static Timetable createLibrary(String id, String schoolId, Period period, WorkingDay day, Room room,
            String color) {
        Instant now = Instant.now();
        return new Timetable(id, schoolId, period, Kind.LIBRARY, null, day, room, color, now, now);
    }

    public boolean isLibrary() {
        return kind == Kind.LIBRARY;
    }

    /** What the slot shows: the subject's name, or "Library". */
    public String label() {
        if (isLibrary()) {
            return LIBRARY_LABEL;
        }
        return teacherSubject != null && teacherSubject.getSubject() != null ? teacherSubject.getSubject().getName()
                : "";
    }

    public void update(TeacherSubject subject, Room room, String color) {
        if (subject == null) {
            throw new IllegalArgumentException("Choose a subject");
        }
        this.kind = Kind.SUBJECT;
        this.teacherSubject = subject;
        this.room = room;
        this.color = color;
        touch();
    }

    /** Turns this entry into (or keeps it as) a Library period. */
    public void updateAsLibrary(Room room, String color) {
        this.kind = Kind.LIBRARY;
        this.teacherSubject = null;
        this.room = room;
        this.color = color;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
