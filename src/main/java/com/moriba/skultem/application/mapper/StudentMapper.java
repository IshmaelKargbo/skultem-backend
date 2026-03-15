package com.moriba.skultem.application.mapper;

import java.time.LocalDate;
import java.time.Period;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Student;

public class StudentMapper {

    public static StudentDTO toDTO(Student param, Enrollment enrollment) {

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
                enrollmentId,
                param.getAdmissionNumber(),
                param.getGivenNames(),
                param.getFamilyName(),
                param.getGender(),
                param.getDateOfBirth(),
                age,
                classId,
                className,
                param.getParent().getUser().getName(),
                param.getParent().getFatherName(),
                param.getParent().getMotherName(),
                param.getStatus(),
                param.getCreatedAt(),
                param.getUpdatedAt()
        );
    }
}