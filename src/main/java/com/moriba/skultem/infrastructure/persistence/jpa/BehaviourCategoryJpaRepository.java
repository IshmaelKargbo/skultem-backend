package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.BehaviourCategoryEntity;

@Repository
public interface BehaviourCategoryJpaRepository extends JpaRepository<BehaviourCategoryEntity, String> {
    Page<BehaviourCategoryEntity> findAllBySchoolIdOrderByCreatedAtAsc(String schoolId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSchoolId(String name, String school);

    Optional<BehaviourCategoryEntity> findByIdAndSchoolId(String id, String schoolId);
}
