package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentScoreJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AssessmentScoreMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssessmentScoreAdapter implements AssessmentScoreRepository {
    private final AssessmentScoreJpaRepository repo;

    @Override
    public void deleteAllByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId) {
        repo.deleteAllByEnrollmentAndSchool(enrollmentId, schoolId);
    }

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
    public List<AssessmentScore> findAllByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId) {
        return repo.findAllByStudentAssessment_Enrollment_IdAndSchoolId(enrollmentId, schoolId).stream()
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
    public boolean existsGradeActivityByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId,
            String schoolId) {
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

    @Override
    public boolean existsGradeActivityByClassIdAndSubjectIdAndAcademicYearIdAndSchoolId(String classId,
            String subjectId, String academicYearId, String schoolId) {
        boolean hasScored = repo
                .existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndStudentAssessment_Enrollment_AcademicYear_IdAndSchoolIdAndScoreGreaterThan(
                        classId,
                        subjectId,
                        academicYearId,
                        schoolId,
                        0);

        if (hasScored) {
            return true;
        }

        return repo
                .existsByStudentAssessment_Enrollment_Clazz_IdAndStudentAssessment_TeacherSubject_Subject_IdAndStudentAssessment_Enrollment_AcademicYear_IdAndSchoolIdAndCycle_StatusNot(
                        classId,
                        subjectId,
                        academicYearId,
                        schoolId,
                        ClassSubjectAssessmentLifeCycle.Status.DRAFT);
    }

    @Override
    public Page<AssessmentScore> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        return repo.runReport(schoolId, filters, pageable)
                .map(AssessmentScoreMapper::toDomain);
    }

    @Override
    public Integer getStudentRank(String schoolId, String classId, String termId, String studentId) {
        return repo.getStudentRank(schoolId, classId, termId, studentId);
    }

    @Override
    public List<Object[]> averageScoresByClassAndTerm(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> excludedStatuses) {
        return repo.averageScoresByClassAndTerm(schoolId, classId, termId, excludedStatuses);
    }

    @Override
    public List<Object[]> studentSubjectAveragesForReport(String schoolId, String classId, String termId,
            String subjectId, List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses) {
        return repo.studentSubjectAveragesForReport(schoolId, classId, termId, subjectId, approvedStatuses);
    }

    @Override
    public List<Object[]> assessmentCompletionByClassAndTerm(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses) {
        return repo.assessmentCompletionByClassAndTerm(schoolId, classId, termId, approvedStatuses);
    }

    @Override
    public List<Object[]> assessmentTrendByEnrollmentAndTerm(String schoolId, String enrollmentId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses) {
        return repo.assessmentTrendByEnrollmentAndTerm(schoolId, enrollmentId, termId, approvedStatuses);
    }

    @Override
    public List<Object[]> assessmentTrendByClassAndTerm(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses) {
        return repo.assessmentTrendByClassAndTerm(schoolId, classId, termId, approvedStatuses);
    }

    @Override
    public List<Object[]> assessmentAverageTrendForReport(String schoolId, String classId, String termId,
            String subjectId, List<ClassSubjectAssessmentLifeCycle.Status> approvedStatuses) {
        return repo.assessmentAverageTrendForReport(schoolId, classId, termId, subjectId, approvedStatuses);
    }

    @Override
    public List<Object[]> averageScoresForAttentionReport(String schoolId, String classId, String termId,
            List<ClassSubjectAssessmentLifeCycle.Status> excludedStatuses) {
        return repo.averageScoresForAttentionReport(schoolId, classId, termId, excludedStatuses);
    }
}
