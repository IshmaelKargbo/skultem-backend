package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.StudentAssessmentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.StudentAssessmentMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentAssessmentAdapter implements StudentAssessmentRepository {
    private final StudentAssessmentJpaRepository repo;

    @Override
    public void save(StudentAssessment domain) {
        repo.save(StudentAssessmentMapper.toEntity(domain));
    }

    @Override
    public void deleteByEnrollmentIdAndSubjectIdAndSchoolId(String enrollmentId, String subjectId, String schoolId) {
        repo.deleteByEnrollment_IdAndTeacherSubject_IdAndSchoolId(enrollmentId, subjectId, schoolId);
    }

    @Override
    public void saveAll(List<StudentAssessment> domain) {
        var entities = domain.stream().map(StudentAssessmentMapper::toEntity).toList();
        repo.saveAll(entities);
    }

    @Override
    public List<StudentAssessment> findAllByTeacherSubjectId(String teacherId) {
        return repo.findAllByTeacherSubject_Id(teacherId).stream()
                .map(StudentAssessmentMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEnrollmentIdAndAndSubjectIdAndSchoolId(String enrollmentId, String termId, String subjectId,
            String schoolId) {
        return existsByEnrollmentIdAndTermIdAndSubjectIdAndSchoolId(enrollmentId, termId, subjectId, schoolId);
    }

    @Override
    public boolean existsByEnrollmentIdAndTermIdAndSubjectIdAndSchoolId(String enrollmentId, String termId,
            String subjectId, String schoolId) {
        return repo.existsByEnrollment_IdAndTerm_IdAndTeacherSubject_Subject_IdAndSchoolId(enrollmentId, termId, subjectId,
                schoolId);
    }

    @Override
    public List<StudentAssessment> findAllByTeacherSubjectIdTermId(String teacherId, String termId) {
        return repo.findAllByTeacherSubject_IdAndTerm_Id(teacherId, termId).stream()
                .map(StudentAssessmentMapper::toDomain)
                .toList();
    }

}
