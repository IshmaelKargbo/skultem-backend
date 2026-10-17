package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ContactMessageEntity;

public interface ContactMessageJpaRepository extends JpaRepository<ContactMessageEntity, UUID> {
    Page<ContactMessageEntity> findAllByStatus(ContactMessageEntity.Status status, Pageable pageable);

    long countByStatus(ContactMessageEntity.Status status);
}
