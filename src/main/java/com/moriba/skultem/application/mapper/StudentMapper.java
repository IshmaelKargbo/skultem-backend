package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Student;

public class StudentMapper {
    public static StudentDTO toDTO(Student param, Enrollment enrollment) {
        String className = "N/A", classId = "";

        if (enrollment != null
                && enrollment.getClazz() != null
                && enrollment.getSection() != null) {

            classId = enrollment.getClazz().getId();

            String baseName = enrollment.getClazz().getName()
                    + " (" + enrollment.getSection().getName();

            if (enrollment.getStream() != null) {
                baseName += " - " + enrollment.getStream().getName();
            }

            className = baseName + ")";
        }

        return new StudentDTO(param.getId(), enrollment.getId(), param.getAdmissionNumber(), param.getGivenNames(), param.getFamilyName(),
                param.getGender(), param.getDateOfBirth(), classId, className, param.getStatus(), param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
