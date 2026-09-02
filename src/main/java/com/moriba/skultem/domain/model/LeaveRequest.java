package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class LeaveRequest extends AggregateRoot<String> {

    private String schoolId;
    private Teacher teacher;
    private Type type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private Status status;
    private String reviewNote;
    private Instant reviewedAt;

    public enum Type {
        ANNUAL,
        SICK,
        EMERGENCY,
        MATERNITY,
        PATERNITY,
        STUDY,
        UNPAID,
        OTHER
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public LeaveRequest(
            String id,
            String schoolId,
            Teacher teacher,
            Type type,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            Status status,
            String reviewNote,
            Instant reviewedAt,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.schoolId = schoolId;
        this.teacher = teacher;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
        this.reviewNote = reviewNote;
        this.reviewedAt = reviewedAt;

        touch(updatedAt);
    }

    public static LeaveRequest create(String id, String schoolId, Teacher teacher, Type type, LocalDate startDate,
            LocalDate endDate, String reason) {

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be on or after the start date");
        }

        Instant now = Instant.now();

        return new LeaveRequest(id, schoolId, teacher, type, startDate, endDate, reason, Status.PENDING, null, null,
                now, now);
    }

    public void approve(String note) {
        if (status != Status.PENDING) {
            throw new BadRequestException("Only a pending request can be approved");
        }

        this.status = Status.APPROVED;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
        touch(Instant.now());
    }

    public void reject(String note) {
        if (status != Status.PENDING) {
            throw new BadRequestException("Only a pending request can be rejected");
        }

        this.status = Status.REJECTED;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
        touch(Instant.now());
    }

    public long durationDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
