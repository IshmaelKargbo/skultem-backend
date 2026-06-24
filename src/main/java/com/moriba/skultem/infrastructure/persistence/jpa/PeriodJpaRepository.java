package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.PeriodEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TimingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodJpaRepository extends JpaRepository<PeriodEntity, String> {
    boolean existsByNameIgnoreCaseAndSessionIdAndSchoolId(String name, String session, String schoolId);

    Optional<PeriodEntity> findBySchoolId(String schoolId);

    List<PeriodEntity> findAllBySessionIdOrderByCreatedAtAsc(String sessionId);
}
