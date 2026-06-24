package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.infrastructure.persistence.entity.WorkingDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkingDayJpaRepository extends JpaRepository<WorkingDayEntity, String> {
    boolean existsByDayAndSchoolId(WorkingDay.Day day, String schoolId);

    Optional<WorkingDayEntity> findByDayAndSchoolId(WorkingDay.Day day, String schoolId);

    List<WorkingDayEntity> findAllBySchoolId(String schoolId);
}
