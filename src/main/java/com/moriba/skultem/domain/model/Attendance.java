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
    // Only used when the school takes two roll calls a day: each roll call's own mark (null = not
    // taken yet). present/late/excused above stay the result for the whole day - see markRoll.
    private RollMark firstRoll;
    private RollMark secondRoll;

    // Ordered from best to worst: the day takes the worst mark of its roll calls.
    public enum RollMark {
        PRESENT,
        LATE,
        EXCUSED,
        ABSENT;

        public static RollMark of(boolean present, boolean excused, boolean late) {
            if (present)
                return PRESENT;
            if (late)
                return LATE;
            if (excused)
                return EXCUSED;
            return ABSENT;
        }
    }

    public Attendance(String id, String school, Enrollment enrollment, LocalDate date, boolean present, boolean excused,
            boolean late, String reason, boolean holiday, String recordedByUserId, Instant createdAt,
            Instant updatedAt) {
        this(id, school, enrollment, date, present, excused, late, reason, holiday, recordedByUserId, null, null,
                createdAt, updatedAt);
    }

    public Attendance(String id, String school, Enrollment enrollment, LocalDate date, boolean present, boolean excused,
            boolean late, String reason, boolean holiday, String recordedByUserId, RollMark firstRoll,
            RollMark secondRoll, Instant createdAt, Instant updatedAt) {
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
        this.firstRoll = firstRoll;
        this.secondRoll = secondRoll;
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
        // Marked for the whole day at once (one roll call, or an admin correcting the day), so any
        // roll call marks no longer describe it.
        this.firstRoll = null;
        this.secondRoll = null;
        touch(Instant.now());
    }

    // Records one roll call (1 or 2) on a two-roll-call day, then works out the day from both: the
    // worst mark wins, so a student is only Present for the day when present at both roll calls,
    // and missing either one makes the day Absent. Until the second roll call is taken the day
    // follows the first, so the register doesn't show everyone absent through the morning.
    public void markRoll(int roll, RollMark mark, String reason, boolean holiday, String recordedByUserId) {
        if (roll == 1) {
            this.firstRoll = mark;
        } else if (roll == 2) {
            this.secondRoll = mark;
        } else {
            throw new IllegalArgumentException("Roll call must be 1 or 2");
        }

        RollMark day = firstRoll;
        if (day == null || (secondRoll != null && secondRoll.ordinal() > day.ordinal())) {
            day = secondRoll;
        }

        this.present = day == RollMark.PRESENT;
        this.late = day == RollMark.LATE;
        this.excused = day == RollMark.EXCUSED;
        if (mark == RollMark.EXCUSED) {
            this.reason = reason;
        } else if (firstRoll != RollMark.EXCUSED && secondRoll != RollMark.EXCUSED) {
            this.reason = null;
        }
        this.holiday = holiday;
        this.recordedByUserId = recordedByUserId;
        touch(Instant.now());
    }

    public RollMark rollMark(int roll) {
        return roll == 1 ? firstRoll : roll == 2 ? secondRoll : null;
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
