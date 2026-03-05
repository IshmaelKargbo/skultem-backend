package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.domain.model.StudentFee;

public class StudentFeeMapper {
    public static StudentFeeDTO toDTO(StudentFee param) {
        if (param == null)
            return null;

        FeeStructureDTO fee = FeeStructureMapper.toDTO(param.getFee());
        EnrollmentDTO enrollment = EnrollmentMapper.toDTO(param.getEnrollment());
        StudentDTO student = StudentMapper.toDTO(param.getStudent(), param.getEnrollment());

        return new StudentFeeDTO(param.getId(), student, fee, enrollment, param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
