package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.vo.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff_management_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffManagementSectionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(nullable = false)
    private String managementSectionId;

    private Instant createdAt;
}
