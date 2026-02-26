package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.SchoolUserEntity;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public class SchoolUserMapper {
    public static SchoolUser toDomain(SchoolUserEntity param) {
        User user = null;

        if (param.getUser() != null) {
            user = UserMapper.toDomain(param.getUser());
        }
        
        return new SchoolUser(param.getId(), param.getSchoolId(), user, param.getRole(),
                param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SchoolUserEntity toEntity(SchoolUser param) {
        UserEntity user = null;

        if (param.getUser() != null) {
            user = UserMapper.toEntity(param.getUser());
        }

        return SchoolUserEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .user(user)
                .role(param.getRole())
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
