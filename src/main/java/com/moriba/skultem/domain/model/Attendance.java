package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Attendance extends AggregateRoot<String> {

    private String schoolId;
    private Enrollment enrollment;
    private LocalDate date;
    private boolean present;
    private boolean excused;
    private Section section;
    private Stream stream;
    private boolean late;
    private String reason;
    private boolean holiday;

    public Attendance(String id, String school, Enrollment enrollment, LocalDate date, boolean present, boolean excused,
            boolean late, String reason, boolean holiday, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.enrollment = enrollment;
        this.schoolId = school;
        this.date = date;
        this.present = present;
        this.excused = excused;
        this.enrollment = enrollment;
        this.holiday = holiday;
        this.reason = reason;
        this.late = late;
        touch(updatedAt);
    }

    public static Attendance create(String id, String school, Enrollment enrollment, LocalDate date, boolean present,
            boolean excused, boolean late, String reason, boolean holiday) {
        Instant now = Instant.now();
        return new Attendance(id, school, enrollment, date, present, excused, late, reason, holiday, now, now);
    }

    public void update(boolean present, boolean excused, boolean late, String reason, boolean holiday) {
        this.present = present;
        this.excused = excused;
        this.late = late;
        this.reason = reason;
        this.holiday = holiday;
        touch(Instant.now());
    }

    public String getStatus() {
        if (isPresent())
            return "Present";
        if (isExcused())
            return "Excused";
        if (isHoliday())
            return "Holiday";
        if (isLate())
            return "Late";
        return "Absent";
    }
}
