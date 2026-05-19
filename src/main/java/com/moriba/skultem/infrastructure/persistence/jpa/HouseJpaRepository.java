package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.HouseEntity;

@Repository
public interface HouseJpaRepository extends JpaRepository<HouseEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Optional<HouseEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<HouseEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
