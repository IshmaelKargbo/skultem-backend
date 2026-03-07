package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ParentEntity;

@Repository
public interface ParentJpaRepository extends JpaRepository<ParentEntity, String> {
    boolean existsByPhoneAndSchoolId(String phone, String schoolId);

    Optional<ParentEntity> findByUserId(String userId);

    Optional<ParentEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<ParentEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
