package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ExamPaper;
import com.moriba.skultem.domain.repository.ExamPaperRepository;
import com.moriba.skultem.infrastructure.persistence.entity.ExamPaperEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.ExamPaperJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ExamPaperAdapter implements ExamPaperRepository {
    private final ExamPaperJpaRepository repo;

    @Override
    public void save(ExamPaper d) {
        repo.save(ExamPaperEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .timetableId(d.getTimetableId())
                .subjectId(d.getSubjectId())
                .paper(d.getPaper())
                .examDate(d.getExamDate())
                .startTime(d.getStartTime())
                .endTime(d.getEndTime())
                .venue(d.getVenue())
                .notes(d.getNotes())
                .classIds(new LinkedHashSet<>(d.getClassIds()))
                .invigilatorIds(new LinkedHashSet<>(d.getInvigilatorIds()))
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build());
    }

    @Override
    public void delete(ExamPaper d) {
        repo.deleteById(d.getId());
    }

    @Override
    public Optional<ExamPaper> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ExamPaperAdapter::toDomain);
    }

    @Override
    public List<ExamPaper> findAllByTimetable(String timetableId) {
        return repo.findAllByTimetableId(timetableId).stream().map(ExamPaperAdapter::toDomain).toList();
    }

    @Override
    public List<ExamPaper> findAllBySchoolIdAndExamDate(String schoolId, LocalDate examDate) {
        return repo.findAllBySchoolIdAndExamDate(schoolId, examDate).stream().map(ExamPaperAdapter::toDomain).toList();
    }

    private static ExamPaper toDomain(ExamPaperEntity e) {
        return new ExamPaper(e.getId(), e.getSchoolId(), e.getTimetableId(), e.getSubjectId(), e.getPaper(),
                e.getExamDate(), e.getStartTime(), e.getEndTime(), e.getVenue(), e.getNotes(), e.getClassIds(),
                e.getInvigilatorIds(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
