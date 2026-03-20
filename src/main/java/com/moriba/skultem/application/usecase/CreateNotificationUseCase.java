package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.BehaviourMapper;
import com.moriba.skultem.application.mapper.NotificationMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.Notification.Type;
import com.moriba.skultem.domain.repository.BehaviourCategoryRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.NotificationRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Kind;
import com.moriba.skultem.domain.vo.Priority;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateNotificationUseCase {
    private final NotificationRepository notificationRepo;

    public NotificationDTO execute(String schoolId, User owner, Type type,
            String title, String message, Map<String, String> meta, Priority priority) {

        Notification notification = Notification.create(
            UUID.randomUUID().toString(),
            schoolId,
            owner,
            type,
            title,
            message,
            meta,
            priority
        );

        notificationRepo.save(notification);
        return NotificationMapper.toDTO(notification);
    }
}