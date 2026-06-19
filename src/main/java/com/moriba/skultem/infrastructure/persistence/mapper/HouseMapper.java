package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.List;

import com.moriba.skultem.domain.model.House;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.infrastructure.persistence.entity.HouseEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherEntity;

public class HouseMapper {
    public static House toDomain(HouseEntity param) {
        if (param == null) {
            return null;
        }

        List<Teacher> houseMasters = param.getHouseMasters().stream()
                .map(TeacherMapper::toDomain)
                .toList();

        return new House(param.getId(), param.getSchoolId(), param.getName(), param.getMotto(), param.getColor(),
                houseMasters, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static HouseEntity toEntity(House param) {
        if (param == null) {
            return null;
        }

        List<TeacherEntity> houseMasters = param.getHouseMasters().stream()
                .map(TeacherMapper::toEntity)
                .toList();

        return HouseEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .color(param.getColor())
                .status(param.getStatus())
                .motto(param.getMotto())
                .houseMasters(houseMasters)
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
