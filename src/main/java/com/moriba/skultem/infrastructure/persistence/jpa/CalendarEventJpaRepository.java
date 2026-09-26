package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.CalendarEventEntity;

public interface CalendarEventJpaRepository extends JpaRepository<CalendarEventEntity, String> {
    Optional<CalendarEventEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<CalendarEventEntity> findAllBySchoolIdOrderByStartDateAsc(String schoolId, Pageable pageable);

    // Whole-school rows plus those for one of the given sections (ids must not be empty).
    @Query("select e from CalendarEventEntity e where e.schoolId = :school and (e.managementSectionId is null "
            + "or e.managementSectionId in :ids) order by e.startDate asc")
    Page<CalendarEventEntity> findVisible(@Param("school") String school, @Param("ids") Collection<String> ids, Pageable pageable);
}
