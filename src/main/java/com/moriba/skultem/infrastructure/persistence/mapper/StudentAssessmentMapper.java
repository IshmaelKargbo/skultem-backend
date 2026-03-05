package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentAssessmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherSubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;

public class StudentAssessmentMapper {
    public static StudentAssessment toDomain(StudentAssessmentEntity param) {
        if (param == null) {
            return null;
        }

        Enrollment enrollment = null;
        TeacherSubject teacherSubject = null;

        if (param.getEnrollment() != null) {
            enrollment = EnrollmentMapper.toDomain(param.getEnrollment());
        }

        if (param.getTeacherSubject() != null) {
            teacherSubject = TeacherSubjectMapper.toDomain(param.getTeacherSubject());
        }

        Term term = null;
        if (param.getTerm() != null) {
            term = TermMapper.toDomain(param.getTerm());
        }

        return new StudentAssessment(param.getId(), param.getSchoolId(), enrollment, teacherSubject, term,
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static StudentAssessmentEntity toEntity(StudentAssessment param) {
        if (param == null) {
            return null;
        }

        EnrollmentEntity enrollment = null;
        TermEntity term = null;
        TeacherSubjectEntity teacherSubject = null;

        if (param.getEnrollment() != null) {
            enrollment = EnrollmentMapper.toEntity(param.getEnrollment());
        }

        if (param.getTeacherSubject() != null) {
            teacherSubject = TeacherSubjectMapper.toEntity(param.getTeacherSubject());
        }

        if (param.getTerm() != null) {
            term = TermMapper.toEntity(param.getTerm());
        }

        return StudentAssessmentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .enrollment(enrollment)
                .teacherSubject(teacherSubject)
                .term(term)
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
