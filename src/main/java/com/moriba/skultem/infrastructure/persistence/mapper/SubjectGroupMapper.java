package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StreamEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectGroupEntity;

public class SubjectGroupMapper {
    public static SubjectGroup toDomain(SubjectGroupEntity param) {
        Stream stream = null;
        Clazz clazz = null;

        if (param.getStream() != null) {
            stream = StreamMapper.toDomain(param.getStream());
        }

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDomain(param.getClazz());
        }

        return new SubjectGroup(param.getId(), param.getSchoolId(), param.getName(), param.getLevel(),
                clazz, stream, param.getRequired(), param.getMinSelection(),
                param.getMaxSelection(), param.getDisplayOrder(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SubjectGroupEntity toEntity(SubjectGroup param) {
        StreamEntity stream = null;
        ClassEntity clazz = null;

        if (param.getStream() != null) {
            stream = StreamMapper.toEntity(param.getStream());
        }

        if (param.getClazz() != null) {
            clazz = ClassMapper.toEntity(param.getClazz());
        }

        return SubjectGroupEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .level(param.getLevel())
                .clazz(clazz)
                .stream(stream)
                .required(param.getRequired())
                .minSelection(param.getMinSelection())
                .maxSelection(param.getMaxSelection())
                .displayOrder(param.getDisplayOrder())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
