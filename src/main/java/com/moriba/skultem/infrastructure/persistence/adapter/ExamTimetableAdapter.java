package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ExamTimetable;
import com.moriba.skultem.domain.repository.ExamTimetableRepository;
import com.moriba.skultem.infrastructure.persistence.entity.ExamTimetableEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.ExamTimetableJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ExamTimetableAdapter implements ExamTimetableRepository {
    private final ExamTimetableJpaRepository repo;

    @Override
    public void save(ExamTimetable d) {
        repo.save(ExamTimetableEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .termId(d.getTermId())
                .assessmentId(d.getAssessmentId())
                .title(d.getTitle())
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .instructions(d.getInstructions())
                .status(d.getStatus())
                .publishedAt(d.getPublishedAt())
                .publishedByName(d.getPublishedByName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build());
    }

    @Override
    public void delete(ExamTimetable d) {
        repo.deleteById(d.getId());
    }

    @Override
    public Optional<ExamTimetable> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ExamTimetableAdapter::toDomain);
    }

    @Override
    public Optional<ExamTimetable> findByTermAndAssessment(String schoolId, String termId, String assessmentId) {
        return repo.findBySchoolIdAndTermIdAndAssessmentId(schoolId, termId, assessmentId)
                .map(ExamTimetableAdapter::toDomain);
    }

    @Override
    public List<ExamTimetable> findAllBySchoolAndTerm(String schoolId, String termId) {
        return repo.findAllBySchoolIdAndTermId(schoolId, termId).stream().map(ExamTimetableAdapter::toDomain).toList();
    }

    private static ExamTimetable toDomain(ExamTimetableEntity e) {
        return new ExamTimetable(e.getId(), e.getSchoolId(), e.getTermId(), e.getAssessmentId(), e.getTitle(),
                e.getStartDate(), e.getEndDate(), e.getInstructions(), e.getStatus(), e.getPublishedAt(),
                e.getPublishedByName(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
