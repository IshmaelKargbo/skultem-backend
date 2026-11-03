package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ExamSchedule;
import com.moriba.skultem.domain.repository.ExamScheduleRepository;
import com.moriba.skultem.infrastructure.persistence.entity.ExamScheduleEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.ExamScheduleJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ExamScheduleAdapter implements ExamScheduleRepository {
    private final ExamScheduleJpaRepository repo;

    @Override
    public void save(ExamSchedule domain) {
        repo.save(toEntity(domain));
    }

    @Override
    public void delete(ExamSchedule domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public Optional<ExamSchedule> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ExamScheduleAdapter::toDomain);
    }

    @Override
    public List<ExamSchedule> findAllBySchoolIdAndExamDate(String schoolId, LocalDate examDate) {
        return repo.findAllBySchoolIdAndExamDate(schoolId, examDate).stream().map(ExamScheduleAdapter::toDomain)
                .toList();
    }

    private static ExamSchedule toDomain(ExamScheduleEntity e) {
        return new ExamSchedule(e.getId(), e.getSchoolId(), e.getTermId(), e.getClassId(), e.getSubjectId(),
                e.getPaper(), e.getExamDate(), e.getStartTime(), e.getEndTime(), e.getVenue(), e.getInvigilatorId(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static ExamScheduleEntity toEntity(ExamSchedule d) {
        return ExamScheduleEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .termId(d.getTermId())
                .classId(d.getClassId())
                .subjectId(d.getSubjectId())
                .paper(d.getPaper())
                .examDate(d.getExamDate())
                .startTime(d.getStartTime())
                .endTime(d.getEndTime())
                .venue(d.getVenue())
                .invigilatorId(d.getInvigilatorId())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
