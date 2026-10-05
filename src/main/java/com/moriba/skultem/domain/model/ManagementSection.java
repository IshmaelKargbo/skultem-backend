package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.GradeApprover;
import com.moriba.skultem.domain.vo.Address;

import lombok.Getter;

// A school-defined management grouping of one or more of its levels - e.g. "Early Years & Primary"
// managing Daycare + Nursery + Primary. Only exists in a SECTION_BASED school; which levels it
// manages is recorded on SchoolLevel. Unrelated to Section (a class division like "A") and Stream.
// It can carry its own logo / principal / signature / address for a school run from different
// places; anything left null falls back to the school's own (see SchoolBrandingResolver).
@Getter
public class ManagementSection extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private int displayOrder;
    private String logo;
    private String principalName;
    private String principalSignature;
    private Address address;
    private String phone;
    // Optional per-section attendance rules; null = use the school's (see AttendanceRulesResolver).
    private Double attendanceThreshold;
    private Integer attendanceWindowDays;
    private Integer attendanceMinDays;
    private Integer attendanceStreakDays;
    // Null = the school's choice (see GradeApprovalResolver).
    private GradeApprover gradeApprover;

    public ManagementSection(String id, String schoolId, String name, int displayOrder, String logo,
            String principalName, String principalSignature, Address address, String phone,
            Double attendanceThreshold, Integer attendanceWindowDays, Integer attendanceMinDays,
            Integer attendanceStreakDays, GradeApprover gradeApprover, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.displayOrder = displayOrder;
        this.logo = logo;
        this.principalName = principalName;
        this.principalSignature = principalSignature;
        this.address = address;
        this.phone = phone;
        this.attendanceThreshold = attendanceThreshold;
        this.attendanceWindowDays = attendanceWindowDays;
        this.attendanceMinDays = attendanceMinDays;
        this.attendanceStreakDays = attendanceStreakDays;
        this.gradeApprover = gradeApprover;
        touch(updatedAt);
    }

    public static ManagementSection create(String schoolId, String name, int displayOrder) {
        Instant now = Instant.now();
        return new ManagementSection(UUID.randomUUID().toString(), schoolId, name, displayOrder, null, null, null, null, null, null, null, null, null, null,
                now, now);
    }

    public void update(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
        touch(Instant.now());
    }

    // Null clears the override, so the section uses the school's choice again.
    public void updateGradeApprover(GradeApprover gradeApprover) {
        this.gradeApprover = gradeApprover;
        touch(Instant.now());
    }

    // A null value clears that override, so the section inherits the school's again.
    public void updateAttendanceRules(Double threshold, Integer windowDays, Integer minDays, Integer streakDays) {
        if (threshold != null && (threshold < 0 || threshold > 100)) {
            throw new RuleException("Attendance threshold must be between 0 and 100");
        }
        if (windowDays != null && (windowDays < 5 || windowDays > 60)) {
            throw new RuleException("Attendance window must be between 5 and 60 school days");
        }
        if (minDays != null && (minDays < 1 || minDays > 60)) {
            throw new RuleException("Minimum recorded days must be between 1 and 60");
        }
        if (windowDays != null && minDays != null && minDays > windowDays) {
            throw new RuleException("Minimum recorded days can't exceed the attendance window");
        }
        if (streakDays != null && (streakDays < 2 || streakDays > 10)) {
            throw new RuleException("Consecutive absences must be between 2 and 10");
        }
        this.attendanceThreshold = threshold;
        this.attendanceWindowDays = windowDays;
        this.attendanceMinDays = minDays;
        this.attendanceStreakDays = streakDays;
        touch(Instant.now());
    }

    public void updateBranding(String logo, String principalName, String principalSignature, Address address,
            String phone) {
        this.logo = logo;
        this.principalName = principalName;
        this.principalSignature = principalSignature;
        this.address = address;
        this.phone = phone;
        touch(Instant.now());
    }
}
