package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.NotificationEntity;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {
    boolean existsBySchoolIdAndOwner_IdAndTypeAndMeta(String schoolId, String userId, String type, String meta);

    Page<NotificationEntity> findAllByOwner_IdAndSchoolId(String userId, String schoolId, Pageable pageable);

    Optional<NotificationEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<NotificationEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
