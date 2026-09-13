package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.TimingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimingJpaRepository extends JpaRepository<TimingEntity, String> {
    Optional<TimingEntity> findByIdAndSchoolId(String id, String schoolId);

    List<TimingEntity> findAllBySchoolId(String schoolId);

    Optional<TimingEntity> findBySchoolIdAndIsDefaultTrue(String schoolId);

    boolean existsBySchoolIdAndIsDefaultTrue(String schoolId);
}
