package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.MaterialTransactionEntity;

public interface MaterialTransactionJpaRepository extends JpaRepository<MaterialTransactionEntity, String> {
    Page<MaterialTransactionEntity> findAllByMaterialIdAndSchoolIdOrderByCreatedAtDesc(String materialId,
            String schoolId, Pageable pageable);
}
