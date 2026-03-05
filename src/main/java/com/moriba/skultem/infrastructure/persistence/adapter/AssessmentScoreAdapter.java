package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentScoreJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AssessmentScoreMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentScoreAdapter implements AssessmentScoreRepository {
    private final AssessmentScoreJpaRepository repo;

    @Override
    public void save(AssessmentScore domain) {
        var entity = AssessmentScoreMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public List<AssessmentScore> findAllByStudentAssessment(String assessmentId) {
        return repo.findAllByStudentAssessment_IdOrderByCycle_Assessment_PositionAsc(assessmentId).stream()
                .map(AssessmentScoreMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existByStudentAssessmentAndCycle(String studentAssessmentId, String cycleId) {
        return repo.existsByStudentAssessment_IdAndCycle_Id(studentAssessmentId, cycleId);
    }

    @Override
    public void saveAll(List<AssessmentScore> domain) {
        var entities = domain.stream().map(AssessmentScoreMapper::toEntity).toList();
        repo.saveAll(entities);
    }

    @Override
    public List<AssessmentScore> findAllByStudentAssessmentIdAndCycle(String studentAssessmentId, String cycleId) {
        return repo
                .findAllByStudentAssessment_IdAndCycle_IdOrderByCycle_Assessment_PositionAsc(studentAssessmentId,
                        cycleId)
                .stream().map(AssessmentScoreMapper::toDomain)
                .toList();
    }

    @Override
    public List<AssessmentScore> findAllByStudentAssessmentIdAndAssessmentId(String studentAssessmentId,
            String assessmentId) {
        return repo
                .findAllByStudentAssessment_IdAndCycle_Assessment_IdOrderByCycle_Assessment_PositionAsc(
                        studentAssessmentId, assessmentId)
                .stream()
                .map(AssessmentScoreMapper::toDomain)
                .toList();
    }

    @Override
    public List<AssessmentScore> findAllByCycle(String cycleId) {
        return repo
                .findAllByCycle_Id(cycleId)
                .stream()
                .map(AssessmentScoreMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsGradeActivityByClassIdAndSchoolId(String classId, String schoolId) {
        boolean hasScored = repo.existsByStudentAssessment_Enrollment_Clazz_IdAndSchoolIdAndScoreGreaterThan(
                classId,
                schoolId,
                0);

        if (hasScored) {
            return true;
        }

        return repo.existsByStudentAssessment_Enrollment_Clazz_IdAndSchoolIdAndCycle_StatusNot(
                classId,
                schoolId,
                ClassSubjectAssessmentLifeCycle.Status.DRAFT);
    }

    @Override
    public boolean existsGradeActivityByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId) {
        boolean hasScored = repo
                .existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndSchoolIdAndScoreGreaterThan(
                        classId,
                        subjectId,
                        schoolId,
                        0);

        if (hasScored) {
            return true;
        }

        return repo
                .existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndSchoolIdAndCycle_StatusNot(
                        classId,
                        subjectId,
                        schoolId,
                        ClassSubjectAssessmentLifeCycle.Status.DRAFT);
    }

}
