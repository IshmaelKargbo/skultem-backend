package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.UserSessionDTO;
import com.moriba.skultem.domain.model.UserSession;

public class UserSessionViewMapper {
    public static UserSessionDTO toDTO(UserSession param) {
        String userId = param.getUser() != null ? param.getUser().getId() : null;
        String userName = param.getUser() != null ? param.getUser().getName() : "System";
        String userEmail = param.getUser() != null ? param.getUser().getEmail() : null;

        return new UserSessionDTO(
                param.getId(),
                userId,
                userName,
                userEmail,
                param.getIpAddress(),
                param.getDevice(),
                param.getDeviceType(),
                param.getOs(),
                param.getBrowser(),
                param.getUserAgent(),
                param.isActive(),
                param.getCreatedAt());
    }
}
