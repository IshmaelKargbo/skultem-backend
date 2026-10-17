package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity;
import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity.Status;

public interface LeadJpaRepository extends JpaRepository<LeadEntity, UUID> {
    Optional<LeadEntity> findByDemoRequestId(UUID demoRequestId);

    Optional<LeadEntity> findByContactMessageId(UUID contactMessageId);

    // The lead an enquiry from this address already belongs to, if one is still being worked.
    List<LeadEntity> findByEmailIgnoreCaseAndStatusNotInOrderByCreatedAtDesc(String email, List<Status> closed);
}
