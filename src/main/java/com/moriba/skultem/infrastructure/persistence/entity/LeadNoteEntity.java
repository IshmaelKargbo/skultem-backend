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

// One entry in a lead's history: a note, a call or email logged by staff, or something the system recorded.
@Entity
@Table(name = "lead_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadNoteEntity {
    public enum Kind {
        NOTE, CALL, EMAIL, MEETING, STATUS_CHANGE, SYSTEM
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    // Null for entries the system made on its own.
    private String createdBy;

    private Instant createdAt;
}
