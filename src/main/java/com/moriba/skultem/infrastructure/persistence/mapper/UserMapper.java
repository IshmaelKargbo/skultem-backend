package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public class UserMapper {
    public static User toDomain(UserEntity param) {
        return new User(param.getId(), param.getGivenName(), param.getFamilyName(), param.getEmail(),
                param.getPassword(), param.getHint(), param.getStatus(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static UserEntity toEntity(User param) {
        return UserEntity.builder()
                .id(param.getId())
                .email(param.getEmail())
                .password(param.getPassword())
                .givenName(param.getGivenNames())
                .hint(param.getHint())
                .familyName(param.getFamilyName())
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
