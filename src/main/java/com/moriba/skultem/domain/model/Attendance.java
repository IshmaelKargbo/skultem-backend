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
        // The register records a late arrival as late=true with present=false (marking someone present AND late is
        // refused, see MarkClassSessionAttendanceUseCase), and every attendance rate counts "present or late" as
        // attending. So late has to be checked on its own - requiring present too meant a late student was reported as
        // Absent everywhere this label is used (reports, the parent calendar, the class page).
        if (isLate())
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
