package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.NoticeEntity;

public interface NoticeJpaRepository extends JpaRepository<NoticeEntity, String> {
    Optional<NoticeEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<NoticeEntity> findAllBySchoolIdOrderByPinnedDescCreatedAtDesc(String schoolId, Pageable pageable);
}
