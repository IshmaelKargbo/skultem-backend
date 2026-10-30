package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.LibrarySessionEntity;

import jakarta.persistence.LockModeType;

public interface LibrarySessionJpaRepository extends JpaRepository<LibrarySessionEntity, String> {

    Optional<LibrarySessionEntity> findByIdAndSchoolId(String id, String schoolId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LibrarySessionEntity s where s.id = :id and s.schoolId = :schoolId")
    Optional<LibrarySessionEntity> findForUpdate(@Param("id") String id, @Param("schoolId") String schoolId);

    Optional<LibrarySessionEntity> findBySchoolIdAndTimetableEntryIdAndDate(String schoolId, String timetableEntryId,
            LocalDate date);

    List<LibrarySessionEntity> findBySchoolIdAndDateBetweenOrderByDateAscStartTimeAsc(String schoolId, LocalDate from,
            LocalDate to);

    List<LibrarySessionEntity> findBySchoolIdAndIdIn(String schoolId, Collection<String> ids);
}
