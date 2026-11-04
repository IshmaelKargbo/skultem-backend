package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public class UserMapper {
    public static User toDomain(UserEntity param) {
        var user = new User(param.getId(), param.getGivenName(), param.getFamilyName(), param.getEmail(),
                param.getPassword(), param.getHint(), param.getStatus(), param.getPhoto(),
                param.getCreatedAt(), param.getUpdatedAt());
        user.restoreEmailVerification(param.isEmailVerified(), param.getEmailVerifiedAt());
        return user;
    }

    public static UserEntity toEntity(User param) {
        if (param == null) {
            return null;
        }

        return UserEntity.builder()
                .id(param.getId())
                .email(param.getEmail())
                .password(param.getPassword())
                .givenName(param.getGivenNames())
                .hint(param.getHint())
                .familyName(param.getFamilyName())
                .status(param.getStatus())
                .photo(param.getPhoto())
                .emailVerified(param.isEmailVerified())
                .emailVerifiedAt(param.getEmailVerifiedAt())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
