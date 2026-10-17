package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

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

// One enquiry being worked towards becoming a school. See LeadService.
@Entity
@Table(name = "leads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadEntity {
    public enum Source {
        DEMO_REQUEST, CONTACT_MESSAGE, MANUAL, REFERRAL, OTHER
    }

    public enum Status {
        NEW, CONTACTED, DEMO_SCHEDULED, DEMO_COMPLETED, INTERESTED, ONBOARDING, CONVERTED, LOST
    }

    public enum DemoStatus {
        NOT_SCHEDULED, SCHEDULED, COMPLETED, CANCELLED
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String schoolName;

    private String email;

    private String phone;

    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    // The enquiry this lead came from - each can belong to at most one lead.
    private UUID demoRequestId;

    private UUID contactMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DemoStatus demoStatus;

    private Instant demoAt;

    // The staff member responsible (a user id).
    private String assignedTo;

    private Instant nextFollowUpAt;

    @Column(columnDefinition = "text")
    private String summary;

    // The school this lead became, once it exists.
    private String schoolId;

    private Instant convertedAt;

    @Column(columnDefinition = "text")
    private String lostReason;

    private Instant createdAt;

    private Instant updatedAt;
}
