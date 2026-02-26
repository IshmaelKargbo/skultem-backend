package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SectionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public class EnrollmentMapper {
    public static Enrollment toDomain(EnrollmentEntity param) {
        Clazz clazz = null;
        AcademicYear academicYear = null;
        Stream stream = null;
        Section section = null;
        Student student = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDomain(param.getClazz());
        }

        if (param.getStream() != null) {
            stream = StreamMapper.toDomain(param.getStream());
        }

        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());
        }

        if (param.getSection() != null) {
            section = SectionMapper.toDomain(param.getSection());
        }

        if (param.getStudent() != null) {
            student = StudentMapper.toDomain(param.getStudent());
        }

        return new Enrollment(param.getId(), param.getSchoolId(), student, clazz, section, academicYear, stream, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static EnrollmentEntity toEntity(Enrollment param) {
        ClassEntity clazz = null;
        AcademicYearEntity academicYear = null;
        SectionEntity section = null;
        StreamEntity stream = null;
        StudentEntity student = null;


        if (param.getStudent() != null) {
            student = StudentMapper.toEntity(param.getStudent());
        }

        if (param.getClazz() != null) {
            clazz = ClassMapper.toEntity(param.getClazz());
        }

        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toEntity(param.getAcademicYear());
        }

        if (param.getSection() != null){
            section = SectionMapper.toEntity(param.getSection());
        }

        if (param.getStream() != null) {
            stream = StreamMapper.toEntity(param.getStream());
        }

        return EnrollmentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .clazz(clazz)
                .academicYear(academicYear)
                .section(section)
                .stream(stream)
                .student(student)
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
