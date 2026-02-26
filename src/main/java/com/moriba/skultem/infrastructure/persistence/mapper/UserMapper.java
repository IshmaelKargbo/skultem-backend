package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public class UserMapper {
    public static User toDomain(UserEntity param) {
        return new User(param.getId(), param.getGivenName(), param.getFamilyName(), param.getEmail(),
                param.getPassword(), param.getStatus(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static UserEntity toEntity(User args) {
        return UserEntity.builder()
                .id(args.getId())
                .email(args.getEmail())
                .password(args.getPassword())
                .givenName(args.getGivenNames())
                .familyName(args.getFamilyName())
                .status(args.getStatus())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
