package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

// Staff (teacher) attendance for a single day - a genuinely separate concept from student
// Attendance (which is keyed off Enrollment, not Teacher).
@Getter
public class TeacherAttendance extends AggregateRoot<String> {

    private String schoolId;
    private Teacher teacher;
    private LocalDate date;
    private Status status;
    private String note;

    // All four null unless this row came from the teacher's own geofenced Clock In/Out, or an
    // admin clocking them in/out on their behalf (an admin's plain status mark leaves them null)
    // - see ClockInUseCase/ClockOutUseCase and AdminClockInUseCase/AdminClockOutUseCase.
    private Instant clockedInAt;
    private String clockInIp;
    private Instant clockedOutAt;
    private String clockOutIp;

    // True when the clock-in/out above was recorded by an admin on the teacher's behalf (e.g. an
    // internet or GPS issue on their end) rather than the teacher's own geofenced self-service.
    private boolean clockInByAdmin;
    private boolean clockOutByAdmin;

    public enum Status {
        PRESENT,
        LATE,
        ABSENT,
        EXCUSED
    }

    public TeacherAttendance(
            String id,
            String schoolId,
            Teacher teacher,
            LocalDate date,
            Status status,
            String note,
            Instant clockedInAt,
            String clockInIp,
            Instant clockedOutAt,
            String clockOutIp,
            boolean clockInByAdmin,
            boolean clockOutByAdmin,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.schoolId = schoolId;
        this.teacher = teacher;
        this.date = date;
        this.status = status;
        this.note = note;
        this.clockedInAt = clockedInAt;
        this.clockInIp = clockInIp;
        this.clockedOutAt = clockedOutAt;
        this.clockOutIp = clockOutIp;
        this.clockInByAdmin = clockInByAdmin;
        this.clockOutByAdmin = clockOutByAdmin;

        touch(updatedAt);
    }

    public static TeacherAttendance mark(String id, String schoolId, Teacher teacher, LocalDate date, Status status,
            String note) {
        Instant now = Instant.now();

        return new TeacherAttendance(id, schoolId, teacher, date, status, note, null, null, null, null, false, false,
                now, now);
    }

    public static TeacherAttendance clockIn(String id, String schoolId, Teacher teacher, LocalDate date,
            String ip) {
        Instant now = Instant.now();

        return new TeacherAttendance(id, schoolId, teacher, date, Status.PRESENT, null, now, ip, null, null, false,
                false, now, now);
    }

    // Same as clockIn(), but recorded by an admin on the teacher's behalf - see AdminClockInUseCase.
    public static TeacherAttendance adminClockIn(String id, String schoolId, Teacher teacher, LocalDate date,
            String ip) {
        Instant now = Instant.now();

        return new TeacherAttendance(id, schoolId, teacher, date, Status.PRESENT, null, now, ip, null, null, true,
                false, now, now);
    }

    public void update(Status status, String note) {
        this.status = status;
        this.note = note;
        touch(Instant.now());
    }

    // For a day that already has a row (e.g. an admin pre-marked it) but hasn't been clocked into
    // yet - turns it into a real clock-in in place, rather than trying to insert a second row for
    // the same (school, teacher, date) and hitting the unique constraint.
    public void applyClockIn(String ip) {
        this.status = Status.PRESENT;
        this.clockedInAt = Instant.now();
        this.clockInIp = ip;
        this.clockInByAdmin = false;
        touch(Instant.now());
    }

    // Same as applyClockIn(), but recorded by an admin on the teacher's behalf.
    public void applyAdminClockIn(String ip) {
        this.status = Status.PRESENT;
        this.clockedInAt = Instant.now();
        this.clockInIp = ip;
        this.clockInByAdmin = true;
        touch(Instant.now());
    }

    public void applyClockOut(String ip) {
        if (!alreadyClockedIn()) {
            throw new BadRequestException("You haven't clocked in today yet");
        }

        this.clockedOutAt = Instant.now();
        this.clockOutIp = ip;
        this.clockOutByAdmin = false;
        touch(Instant.now());
    }

    // Same as applyClockOut(), but recorded by an admin on the teacher's behalf.
    public void applyAdminClockOut(String ip) {
        if (!alreadyClockedIn()) {
            throw new BadRequestException("This teacher hasn't been clocked in today yet");
        }

        this.clockedOutAt = Instant.now();
        this.clockOutIp = ip;
        this.clockOutByAdmin = true;
        touch(Instant.now());
    }

    public boolean alreadyClockedIn() {
        return clockedInAt != null;
    }

    public boolean alreadyClockedOut() {
        return clockedOutAt != null;
    }
}
