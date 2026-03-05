package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.application.dto.SectionDTO;
import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.domain.model.Enrollment;

public class EnrollmentMapper {
    public static EnrollmentDTO toDTO(Enrollment param) {
        if (param == null) {
            return null;
        }

        ClassDTO clazz = null;
        StreamDTO stream = null;
        SectionDTO section = null;
        StudentDTO student = null;
        AcademicYearDTO academic = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDTO(param.getClazz());
        }

        if (param.getStream() != null) {
            stream = StreamMapper.toDTO(param.getStream());
        }

        if (param.getSection() != null) {
            section = SectionMapper.toDTO(param.getSection());
        }

        if (param.getStudent() != null) {
            student = StudentMapper.toDTO(param.getStudent(), param);
        }

        if (param.getAcademicYear() != null) {
            academic = AcademicYearMapper.toDTO(param.getAcademicYear());
        }
       
        return new EnrollmentDTO(param.getId(), param.getSchoolId(), academic, student, clazz, stream, section, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
