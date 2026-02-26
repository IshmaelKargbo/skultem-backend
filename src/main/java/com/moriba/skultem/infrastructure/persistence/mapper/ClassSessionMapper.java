package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SectionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;

public class ClassSessionMapper {
    public static ClassSession toDomain(ClassSessionEntity param) {
        Clazz clazz = null;
        Stream stream = null;
        Section section = null;
        AcademicYear academicYear = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDomain(param.getClazz());
        }

        if (param.getStream() != null) {
            stream = StreamMapper.toDomain(param.getStream());
        }

        if (param.getSection() != null) {
            section = SectionMapper.toDomain(param.getSection());
        }

        if (param.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());
        }

        return new ClassSession(param.getId(), param.getSchoolId(), clazz, stream, section,
                academicYear, param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ClassSessionEntity toEntity(ClassSession args) {
        ClassEntity clazz = null;
        AcademicYearEntity academicYear = null;
        SectionEntity section = null;
        StreamEntity stream = null;

        if (args.getClazz() != null) {
            clazz = ClassMapper.toEntity(args.getClazz());
        }

        if (args.getAcademicYear() != null) {
            academicYear = AcademicYearMapper.toEntity(args.getAcademicYear());
        }

        if (args.getSection() != null){
            section = SectionMapper.toEntity(args.getSection());
        }

        if (args.getStream() != null) {
            stream = StreamMapper.toEntity(args.getStream());
        }

        return ClassSessionEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .clazz(clazz)
                .academicYear(academicYear)
                .section(section)
                .stream(stream)
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
