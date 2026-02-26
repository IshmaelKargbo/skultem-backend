package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.domain.model.Student;

public class StudentMapper {
    public static StudentDTO toDTO(Student param) {
        return new StudentDTO(param.getId(), param.getAdmissionNumber(), param.getGivenNames(), param.getFamilyName(), param.getGender(), param.getDateOfBirth(), param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
