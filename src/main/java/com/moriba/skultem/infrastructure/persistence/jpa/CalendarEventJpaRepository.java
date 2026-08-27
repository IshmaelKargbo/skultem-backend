package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.CalendarEventEntity;

public interface CalendarEventJpaRepository extends JpaRepository<CalendarEventEntity, String> {
    Optional<CalendarEventEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<CalendarEventEntity> findAllBySchoolIdOrderByStartDateAsc(String schoolId, Pageable pageable);
}
