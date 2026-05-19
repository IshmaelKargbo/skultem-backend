package com.moriba.skultem.application.services;

import java.time.Instant;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.application.dto.NotificationPayload;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public void openNotification(String schoolId, String id) {
        var res = notificationRepo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("notification not found"));
        res.markAsRead();
        notificationRepo.save(res);
        var user = res.getOwner();
        long count = notificationRepo.countAllOpenByOwnerAndSchoolId(user.getId(), res.getSchoolId());
        sendToUser(user.getId(), count);
    }

    public void sendToUser(String userId, long count) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications",
                new NotificationPayload(count, Instant.now()));
    }

    public void sendToUser(String userId, long count, NotificationDTO notification) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications",
                new NotificationPayload(count, Instant.now(), notification));
    }
}
