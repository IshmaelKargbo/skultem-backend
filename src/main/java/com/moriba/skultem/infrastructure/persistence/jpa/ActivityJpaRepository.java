package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ActivityEntity;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, String> {
    Page<ActivityEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
