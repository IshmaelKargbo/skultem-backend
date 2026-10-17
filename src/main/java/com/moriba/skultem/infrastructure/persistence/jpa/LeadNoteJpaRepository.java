package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.LeadNoteEntity;

public interface LeadNoteJpaRepository extends JpaRepository<LeadNoteEntity, UUID> {
    List<LeadNoteEntity> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
