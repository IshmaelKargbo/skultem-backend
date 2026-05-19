package com.moriba.skultem.application.mapper;

import java.time.LocalDate;
import java.time.Period;

import com.moriba.skultem.application.dto.FeeDetail;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Student;

public class StudentMapper {

    public static StudentDTO toDTO(Student param, Enrollment enrollment, String relationship) {

        String className = "N/A";
        String classId = "";
        String enrollmentId = null;
        String sessionId = param.getSession() != null ? param.getSession().getId() : null;

        if (enrollment != null) {

            enrollmentId = enrollment.getId();
            
            if (enrollment.getClazz() != null && enrollment.getSection() != null) {

                classId = enrollment.getClazz().getId();

                String baseName = enrollment.getClazz().getName()
                        + " (" + enrollment.getSection().getName();

                if (enrollment.getStream() != null) {
                    baseName += " - " + enrollment.getStream().getName();
                }

                className = baseName + ")";
            }
        }

        Integer age = null;
        if (param.getDateOfBirth() != null) {
            age = Period.between(param.getDateOfBirth(), LocalDate.now()).getYears();
        }

        return new StudentDTO(
                param.getId(),
                param.getPhoto(),
                enrollmentId,
                param.getAdmissionNumber(),
                param.getGivenNames(),
                param.getFamilyName(),
                param.getGender(),
                param.getDateOfBirth(),
                age,
                classId,
                0,
                className,
                "",
                param.getNationality(),
                param.getReligion(),
                param.getCity(),
                param.getStreet(),
                param.getFamily(),
                ParentMapper.toDTO(param.getParent()),
                relationship,
                sessionId,
                param.getStatus(),
                null,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static StudentDTO toDTO(Student param, Enrollment enrollment) {

        String className = "N/A";
        String classId = "";
        String enrollmentId = null;
        String sessionId = param.getSession() != null ? param.getSession().getId() : null;

        if (enrollment != null) {

            enrollmentId = enrollment.getId();
            
            if (enrollment.getClazz() != null && enrollment.getSection() != null) {

                classId = enrollment.getClazz().getId();

                String baseName = enrollment.getClazz().getName()
                        + " (" + enrollment.getSection().getName();

                if (enrollment.getStream() != null) {
                    baseName += " - " + enrollment.getStream().getName();
                }

                className = baseName + ")";
            }
        }

        Integer age = null;
        if (param.getDateOfBirth() != null) {
            age = Period.between(param.getDateOfBirth(), LocalDate.now()).getYears();
        }

        return new StudentDTO(
                param.getId(),
                param.getPhoto(),
                enrollmentId,
                param.getAdmissionNumber(),
                param.getGivenNames(),
                param.getFamilyName(),
                param.getGender(),
                param.getDateOfBirth(),
                age,
                classId,
                0,
                className,
                "",
                param.getNationality(),
                param.getReligion(),
                param.getCity(),
                param.getStreet(),
                param.getFamily(),
                ParentMapper.toDTO(param.getParent()),
                "",
                sessionId,
                param.getStatus(),
                null,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static StudentDTO toDTO(Student param, Enrollment enrollment, FeeDetail feeDetail) {

        String className = "N/A";
        String classId = "";
        String enrollmentId = null;

        if (enrollment != null) {

            enrollmentId = enrollment.getId();

            if (enrollment.getClazz() != null && enrollment.getSection() != null) {

                classId = enrollment.getClazz().getId();

                String baseName = enrollment.getClazz().getName()
                        + " (" + enrollment.getSection().getName();

                if (enrollment.getStream() != null) {
                    baseName += " - " + enrollment.getStream().getName();
                }

                className = baseName + ")";
            }
        }

        Integer age = null;
        if (param.getDateOfBirth() != null) {
            age = Period.between(param.getDateOfBirth(), LocalDate.now()).getYears();
        }

        return new StudentDTO(
                param.getId(),
                param.getPhoto(),
                enrollmentId,
                param.getAdmissionNumber(),
                param.getGivenNames(),
                param.getFamilyName(),
                param.getGender(),
                param.getDateOfBirth(),
                age,
                classId,
                0,
                className,
                "",
                param.getNationality(),
                param.getReligion(),
                param.getCity(),
                param.getStreet(),
                param.getFamily(),
                ParentMapper.toDTO(param.getParent()),
                "",
                "",
                param.getStatus(),
                feeDetail,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static StudentDTO toDTO(Student param, Enrollment enrollment, String teacher, int classSize,
            String sessionId, FeeDetail feeDetail) {

        String className = "N/A";
        String classId = "";
        String enrollmentId = null;

        if (enrollment != null) {

            enrollmentId = enrollment.getId();

            if (enrollment.getClazz() != null && enrollment.getSection() != null) {

                classId = enrollment.getClazz().getId();

                String baseName = enrollment.getClazz().getName()
                        + " (" + enrollment.getSection().getName();

                if (enrollment.getStream() != null) {
                    baseName += " - " + enrollment.getStream().getName();
                }

                className = baseName + ")";
            }
        }

        Integer age = null;
        if (param.getDateOfBirth() != null) {
            age = Period.between(param.getDateOfBirth(), LocalDate.now()).getYears();
        }

        return new StudentDTO(
                param.getId(),
                param.getPhoto(),
                enrollmentId,
                param.getAdmissionNumber(),
                param.getGivenNames(),
                param.getFamilyName(),
                param.getGender(),
                param.getDateOfBirth(),
                age,
                classId,
                classSize,
                className,
                teacher,
                param.getNationality(),
                param.getReligion(),
                param.getCity(),
                param.getStreet(),
                param.getFamily(),
                ParentMapper.toDTO(param.getParent()),
                "",
                sessionId,
                param.getStatus(),
                feeDetail,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static StudentDTO toDTO(Student param, Enrollment enrollment, String teacher, int classSize,
            String sessionId) {

        String className = "N/A";
        String classId = "";
        String enrollmentId = null;

        if (enrollment != null) {

            enrollmentId = enrollment.getId();

            if (enrollment.getClazz() != null && enrollment.getSection() != null) {

                classId = enrollment.getClazz().getId();

                String baseName = enrollment.getClazz().getName()
                        + " (" + enrollment.getSection().getName();

                if (enrollment.getStream() != null) {
                    baseName += " - " + enrollment.getStream().getName();
                }

                className = baseName + ")";
            }
        }

        Integer age = null;
        if (param.getDateOfBirth() != null) {
            age = Period.between(param.getDateOfBirth(), LocalDate.now()).getYears();
        }

        return new StudentDTO(
                param.getId(),
                param.getPhoto(),
                enrollmentId,
                param.getAdmissionNumber(),
                param.getGivenNames(),
                param.getFamilyName(),
                param.getGender(),
                param.getDateOfBirth(),
                age,
                classId,
                classSize,
                className,
                teacher,
                param.getNationality(),
                param.getReligion(),
                param.getCity(),
                param.getStreet(),
                param.getFamily(),
                ParentMapper.toDTO(param.getParent()),
                "",
                sessionId,
                param.getStatus(),
                null,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}