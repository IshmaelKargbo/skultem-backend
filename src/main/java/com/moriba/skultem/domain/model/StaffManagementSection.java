package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Role;

import lombok.Getter;

// One management section a staff member can work in under one of their roles. A (school, user,
// role) with no rows is whole-school - see V62 for why this is keyed by role, not school_users row.
@Getter
public class StaffManagementSection extends AggregateRoot<String> {

    private String schoolId;
    private String userId;
    private Role role;
    private String managementSectionId;

    public StaffManagementSection(String id, String schoolId, String userId, Role role, String managementSectionId,
            Instant createdAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.userId = userId;
        this.role = role;
        this.managementSectionId = managementSectionId;
    }

    public static StaffManagementSection create(String schoolId, String userId, Role role,
            String managementSectionId) {
        return new StaffManagementSection(UUID.randomUUID().toString(), schoolId, userId, role, managementSectionId,
                Instant.now());
    }
}
