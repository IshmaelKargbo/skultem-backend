package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ExamPaperEntity;

public interface ExamPaperJpaRepository extends JpaRepository<ExamPaperEntity, String> {
    Optional<ExamPaperEntity> findByIdAndSchoolId(String id, String schoolId);

    List<ExamPaperEntity> findAllByTimetableId(String timetableId);

    List<ExamPaperEntity> findAllBySchoolIdAndExamDate(String schoolId, LocalDate examDate);
}
