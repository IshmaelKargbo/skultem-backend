package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends AggregateRoot<String> {

    private final String action;
    private final String schoolId;
    private final AcademicYear academicYear;
    private final User user;
    private final String ipAddress;
    private final String device;
    private final String deviceType;
    private final String os;
    private final String browser;
    private final Status status;
    private final String details;

    public enum Status {
        SUCCESS, FAILURE
    }

    public AuditLog(
            String id,
            String action,
            String schoolId,
            AcademicYear academicYear,
            User user,
            String ipAddress,
            String device,
            String deviceType,
            String os,
            String browser,
            Status status,
            String details,
            Instant createdAt,
            Instant updatedAt) {

        super(id, createdAt);
        this.action = action;
        this.schoolId = schoolId;
        this.academicYear = academicYear;
        this.user = user;
        this.ipAddress = ipAddress;
        this.device = device;
        this.deviceType = deviceType;
        this.os = os;
        this.browser = browser;
        this.status = status;
        this.details = details;
        touch(updatedAt);
    }

    public static AuditLog create(
            String id,
            String action,
            String schoolId,
            AcademicYear academicYear,
            User user,
            String ipAddress,
            String device,
            String deviceType,
            String os,
            String browser,
            Status status,
            String details) {

        Instant now = Instant.now();
        return new AuditLog(
                id,
                action,
                schoolId,
                academicYear,
                user,
                ipAddress,
                device,
                deviceType,
                os,
                browser,
                status,
                details,
                now,
                now);
    }
}