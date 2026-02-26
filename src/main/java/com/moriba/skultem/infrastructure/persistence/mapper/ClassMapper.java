package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;

public class ClassMapper {
    public static Clazz toDomain(ClassEntity param) {
        if (param == null) {
            return null;
        }

        Clazz nextClass = null;

        if (param.getNextClass() != null) {
            nextClass = toDomain(param.getNextClass());
        }

        return new Clazz(
                param.getId(),
                param.getSchoolId(),
                param.getName(),
                param.getLevel(),
                param.getLevelOrder(),
                nextClass,
                param.isTerminal(),
                param.getStatus(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static ClassEntity toEntity(Clazz param) {
        if (param == null) {
            return null;
        }

        ClassEntity nextClass = null;

        if (param.getNextClass() != null) {
            nextClass = toEntity(param.getNextClass());
        }

        return ClassEntity.builder()
                .id(param.getId())
                .name(param.getName())
                .schoolId(param.getSchoolId())
                .levelOrder(param.getDisplayOrder())
                .level(param.getLevel())
                .nextClass(nextClass)
                .terminal(Boolean.TRUE.equals(param.getTerminal()))
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }

}
