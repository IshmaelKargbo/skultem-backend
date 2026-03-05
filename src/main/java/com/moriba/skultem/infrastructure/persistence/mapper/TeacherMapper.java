package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherEntity;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public class TeacherMapper {
    public static Teacher toDomain(TeacherEntity param) {
        User user = null;

        if (param.getUser() != null) {
            user = UserMapper.toDomain(param.getUser());
        }

        return new Teacher(param.getId(), param.getSchoolId(), param.getPhone(), param.getStreet(), param.getCity(), param.getStaffId(),
                user, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static TeacherEntity toEntity(Teacher param) {
        UserEntity user = null;

        if (param.getUser() != null) {
            user = UserMapper.toEntity(param.getUser());
        }
        return TeacherEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .user(user)
                .city(param.getCity())
                .street(param.getStreet())
                .staffId(param.getStaffId())
                .phone(param.getPhone())
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
