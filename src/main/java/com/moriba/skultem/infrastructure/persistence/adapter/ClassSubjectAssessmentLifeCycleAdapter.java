package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ClassSubjectAssessmentLifeCycleJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ClassSubjectAssessmentLifeCycleMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassSubjectAssessmentLifeCycleAdapter implements ClassSubjectAssessmentLifeCycleRepository {

    private final ClassSubjectAssessmentLifeCycleJpaRepository repo;

    @Override
    public void save(ClassSubjectAssessmentLifeCycle domain) {
        var entity = ClassSubjectAssessmentLifeCycleMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void saveAll(List<ClassSubjectAssessmentLifeCycle> domain) {
        var entities = domain.stream().map(ClassSubjectAssessmentLifeCycleMapper::toEntity).toList();
        repo.saveAll(entities);
    }

    @Override
    public boolean existByTermAndAssessmentAndTeacherSubject(String termId, String assessmentId,
            String teacherSubjectId) {
        return repo.existsByTerm_IdAndAssessment_IdAndSubject_Id(termId, assessmentId, teacherSubjectId);
    }

    @Override
    public List<ClassSubjectAssessmentLifeCycle> findAllBySubjectAndTerm(String subjectId, String tearmId) {
        return repo.findAllBySubject_IdAndTerm_IdOrderByAssessment_PositionAsc(subjectId, tearmId).stream()
                .map(ClassSubjectAssessmentLifeCycleMapper::toDomain)
                .toList();
    }

    @Override
    public List<ClassSubjectAssessmentLifeCycle> findAllBySchoolAndTerm(String schoolId, String termId) {
        return repo.findAllBySchoolIdAndTerm_IdOrderByAssessment_PositionAsc(schoolId, termId).stream()
                .map(ClassSubjectAssessmentLifeCycleMapper::toDomain)
                .toList();
    }

    @Override
    public List<ClassSubjectAssessmentLifeCycle> findAllBySchoolTermAndPosition(String schoolId, String termId, int position) {
        return repo.findAllBySchoolIdAndTerm_IdAndAssessment_PositionOrderBySubject_Id(schoolId, termId, position).stream()
                .map(ClassSubjectAssessmentLifeCycleMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ClassSubjectAssessmentLifeCycle> findByTeacherSubjectAndAssessmentAndTerm(String subectId,
            String assessmentId, String termId) {
        return repo.findBySubject_IdAndAssessment_IdAndTerm_Id(subectId, assessmentId, termId)
                .map(ClassSubjectAssessmentLifeCycleMapper::toDomain);
    }

}
