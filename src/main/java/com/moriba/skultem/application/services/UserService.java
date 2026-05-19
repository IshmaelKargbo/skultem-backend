package com.moriba.skultem.application.services;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.usecase.AssignRoleUseCase;
import com.moriba.skultem.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final NotificationRepository notificationRepo;
    private final AssignRoleUseCase assignRoleUseCase;

    public long openNotifications(String userId, String schoolId) {
        return notificationRepo.countAllOpenByOwnerAndSchoolId(userId, schoolId);
    }

    public UserDTO assignRole(String school, String userId, String role) {
        return assignRoleUseCase.execute(school, userId, role);
    }
}
