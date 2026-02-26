package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSectionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SectionEntity;

public class ClassSectionMapper {
    public static ClassSection toDomain(ClassSectionEntity param) {
        Clazz clazz = null;
        Section section = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDomain(param.getClazz());
        }

        if (param.getSection() != null) {
            section = SectionMapper.toDomain(param.getSection());
        }

        return new ClassSection(param.getId(), param.getSchoolId(), clazz, section,
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ClassSectionEntity toEntity(ClassSection args) {
        ClassEntity clazz = null;
        SectionEntity section = null;

        if (args.getClass() != null) {
            clazz = ClassMapper.toEntity(args.getClazz());
        }

        if (args.getSection() != null) {
            section = SectionMapper.toEntity(args.getSection());
        }

        return ClassSectionEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .clazz(clazz)
                .section(section)
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
