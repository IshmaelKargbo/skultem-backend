package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.ParentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public class ParentMapper {
    public static Parent toDomain(ParentEntity param) {
        User user = null;

        if (param.getUser() != null) {
            user = UserMapper.toDomain(param.getUser());
        }

        return new Parent(param.getId(), param.getSchoolId(), param.getPhone(), param.getStreet(), param.getCity(), param.getFatherName(), param.getMotherName(),
                user, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ParentEntity toEntity(Parent param) {
        UserEntity user = null;

        if (param.getUser() != null) {
            user = UserMapper.toEntity(param.getUser());
        }

        return ParentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .user(user)
                .city(param.getCity())
                .street(param.getStreet())
                .fatherName(param.getFatherName())
                .motherName(param.getMotherName())
                .phone(param.getPhone())
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
