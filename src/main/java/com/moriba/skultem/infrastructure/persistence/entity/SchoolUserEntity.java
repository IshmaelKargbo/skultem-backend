package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.SchoolUser.Status;
import com.moriba.skultem.domain.vo.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "school_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolUserEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Instant createdAt;
    private Instant updatedAt;
}
