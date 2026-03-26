package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.vo.Role;

public class UserMapper {
    public static UserDTO toDTO(User param) {
        return new UserDTO(param.getId(), param.getName(), param.getGivenNames(), param.getFamilyName(),
                param.getEmail(),
                param.getStatus().toString(), null, param.getCreatedAt(), param.getUpdatedAt());
    }

    public static UserDTO toDTO(User user, List<Role> roles) {
        return new UserDTO(user.getId(), user.getName(), user.getGivenNames(), user.getFamilyName(), user.getEmail(),
                user.getStatus().toString(), roles,
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
