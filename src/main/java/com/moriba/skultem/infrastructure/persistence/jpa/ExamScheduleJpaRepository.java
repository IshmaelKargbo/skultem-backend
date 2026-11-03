package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExamScheduleEntity;

public interface ExamScheduleJpaRepository extends JpaRepository<ExamScheduleEntity, String> {
    Optional<ExamScheduleEntity> findByIdAndSchoolId(String id, String schoolId);

    List<ExamScheduleEntity> findAllBySchoolIdAndExamDate(String schoolId, LocalDate examDate);
}
