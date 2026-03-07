package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.model.SchoolUser;

public class UserMapper {
    public static UserDTO toDTO(User param) {
        return new UserDTO(param.getId(), param.getName(), param.getGivenNames(), param.getFamilyName(), param.getEmail(),
                param.getStatus().toString(), null, null, param.getCreatedAt(), param.getUpdatedAt());
    }

    public static UserDTO toDTO(User user, SchoolUser schoolUser) {
        Role role = schoolUser.getRole();
        return new UserDTO(user.getId(), user.getName(), user.getGivenNames(), user.getFamilyName(), user.getEmail(),
                user.getStatus().toString(), schoolUser.getSchoolId(), role.toString(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
