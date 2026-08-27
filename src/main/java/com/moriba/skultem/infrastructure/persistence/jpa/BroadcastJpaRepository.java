package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.BroadcastEntity;

public interface BroadcastJpaRepository extends JpaRepository<BroadcastEntity, String> {
    Optional<BroadcastEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<BroadcastEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
