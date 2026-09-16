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
    private String recordedByUserId;

    public Attendance(String id, String school, Enrollment enrollment, LocalDate date, boolean present, boolean excused,
            boolean late, String reason, boolean holiday, String recordedByUserId, Instant createdAt,
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
        this.recordedByUserId = recordedByUserId;
        touch(updatedAt);
    }

    public static Attendance create(String id, String school, Enrollment enrollment, LocalDate date, boolean present,
            boolean excused, boolean late, String reason, boolean holiday, String recordedByUserId) {
        Instant now = Instant.now();
        return new Attendance(id, school, enrollment, date, present, excused, late, reason, holiday,
                recordedByUserId, now, now);
    }

    public void update(boolean present, boolean excused, boolean late, String reason, boolean holiday,
            String recordedByUserId) {
        this.present = present;
        this.excused = excused;
        this.late = late;
        this.reason = reason;
        this.holiday = holiday;
        this.recordedByUserId = recordedByUserId;
        touch(Instant.now());
    }

    public String getStatus() {
        // A "late" mark is recorded as present=true, late=true (the student did show up, just
        // late) - checking present first, as this used to, always won and reported "Present"
        // before late was ever consulted, silently hiding every late arrival (from this school's
        // reports, the parent portal calendar, everywhere getStatus() is used) behind a 100%
        // present rate.
        if (isPresent() && isLate())
            return "Late";
        if (isPresent())
            return "Present";
        if (isExcused())
            return "Excused";
        if (isHoliday())
            return "Holiday";
        return "Absent";
    }
}
