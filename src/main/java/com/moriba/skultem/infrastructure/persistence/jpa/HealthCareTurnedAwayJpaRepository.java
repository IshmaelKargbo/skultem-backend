package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.HealthCareTurnedAwayEntity;

public interface HealthCareTurnedAwayJpaRepository extends JpaRepository<HealthCareTurnedAwayEntity, String> {

    List<HealthCareTurnedAwayEntity> findBySchoolIdAndTurnedAwayAtGreaterThanEqualAndTurnedAwayAtLessThanOrderByTurnedAwayAtDesc(
            String schoolId, Instant from, Instant to);

    List<HealthCareTurnedAwayEntity> findBySchoolIdAndStudentIdOrderByTurnedAwayAtDesc(String schoolId, String studentId);
}
