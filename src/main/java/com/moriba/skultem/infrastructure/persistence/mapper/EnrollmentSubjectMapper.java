package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.EnrollmentSubject;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentSubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectEntity;

public class EnrollmentSubjectMapper {
    public static EnrollmentSubject toDomain(EnrollmentSubjectEntity param) {
        Enrollment enrollment = null;
        Subject subject = null;
        Student student = null;

        if (param.getEnrollment() != null) {
            enrollment = EnrollmentMapper.toDomain(param.getEnrollment());
        }

        if (param.getSubject() != null) {
            subject = SubjectMapper.toDomain(param.getSubject());
        }

        if (param.getStudent() != null) {
            student = StudentMapper.toDomain(param.getStudent());
        }

        return new EnrollmentSubject(param.getId(), param.getSchoolId(), enrollment, subject,
                student, param.getCreatedAt(), param.getUpdatedAt());
    }

    public static EnrollmentSubjectEntity toEntity(EnrollmentSubject args) {
        EnrollmentEntity enrollment = null;
        SubjectEntity subject = null;
        StudentEntity student = null;

        if (args.getStudent() != null) {
            student = StudentMapper.toEntity(args.getStudent());
        }

        if (args.getSubject() != null) {
            subject = SubjectMapper.toEntity(args.getSubject());
        }

        if (args.getEnrollment() != null) {
            enrollment = EnrollmentMapper.toEntity(args.getEnrollment());
        }

        if (args.getStudent() != null) {
            student = StudentMapper.toEntity(args.getStudent());
        }

        return EnrollmentSubjectEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .enrollment(enrollment)
                .subject(subject)
                .student(student)
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
