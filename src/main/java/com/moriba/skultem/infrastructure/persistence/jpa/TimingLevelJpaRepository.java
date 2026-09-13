package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.persistence.entity.TimingLevelEntity;

public interface TimingLevelJpaRepository extends JpaRepository<TimingLevelEntity, String> {
    Optional<TimingLevelEntity> findBySchoolIdAndLevel(String schoolId, Level level);

    List<TimingLevelEntity> findAllBySchoolId(String schoolId);

    void deleteByTiming_Id(String timingId);
}
