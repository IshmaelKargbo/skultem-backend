package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.ParentEntity;

public class ParentMapper {
    public static Parent toDomain(ParentEntity param) {
        User user = null;

        if (param.getUser() != null) {
            user = UserMapper.toDomain(param.getUser());
        }

        return new Parent(param.getId(), param.getSchoolId(), param.getPhone(), param.getStreet(), param.getCity(), user, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ParentEntity toEntity(Parent param) {
        if (param == null) {
            return null;
        }

        return ParentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .user(UserMapper.toEntity(param.getUser()))
                .city(param.getCity())
                .street(param.getStreet())
                .phone(param.getPhone())
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
