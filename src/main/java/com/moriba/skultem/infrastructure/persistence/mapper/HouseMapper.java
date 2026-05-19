package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.House;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.infrastructure.persistence.entity.HouseEntity;

public class HouseMapper {
    public static House toDomain(HouseEntity param) {
        if (param == null) {
            return null;
        }

        Teacher houseMaster = TeacherMapper.toDomain(param.getHouseMaster());

        return new House(param.getId(), param.getSchoolId(), param.getName(), param.getMotto(), param.getColor(),
                houseMaster, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static HouseEntity toEntity(House param) {
        if (param == null) {
            return null;
        }

        return HouseEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .color(param.getColor())
                .status(param.getStatus())
                .motto(param.getMotto())
                .houseMaster(TeacherMapper.toEntity(param.getHouseMaster()))
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
