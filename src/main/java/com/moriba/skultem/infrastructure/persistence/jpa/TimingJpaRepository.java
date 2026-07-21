package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.TimingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimingJpaRepository extends JpaRepository<TimingEntity, String> {
    boolean existsBySchoolId(String schoolId);

    Optional<TimingEntity> findBySchoolId(String schoolId);

    Optional<TimingEntity> findBy(String schoolId);
}
