package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.SupplyEntity;

@Repository
public interface SupplyJpaRepository extends JpaRepository<SupplyEntity, String> {
    Page<SupplyEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Page<SupplyEntity> findAllByStudentIdAndSchoolIdOrderByCreatedAtDesc(String studentId, String schoolId,
            Pageable pageable);

    boolean existsByStudentIdAndMaterialIdAndSchoolId(String studentId, String materialId, String schoolId);

    Optional<SupplyEntity> findByIdAndSchoolId(String id, String schoolId);
}
