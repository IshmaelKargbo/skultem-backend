package com.moriba.skultem.infrastructure.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.moriba.skultem.application.events.GradesReleasedEvent;
import com.moriba.skultem.application.events.SupplyCollectedEvent;
import com.moriba.skultem.application.usecase.CreateNotificationUseCase;
import com.moriba.skultem.domain.model.Notification.Type;
import com.moriba.skultem.domain.vo.Priority;

@Component
public class NotificationListener {

    private final CreateNotificationUseCase createNotificationUseCase;

    public NotificationListener(
            CreateNotificationUseCase createNotificationUseCase) {
        this.createNotificationUseCase = createNotificationUseCase;
    }

    @Async
    @EventListener
    public void handleGradesReleased(GradesReleasedEvent event) {
        String message = String.format(
                "Your child %s has a new grade for %s (%s):\n%s: %s",
                event.getStudentName(),
                event.getAssessmentName(),
                event.getTermName(),
                event.getSubjectName(),
                event.getScore());
        
        createNotificationUseCase.execute(
                event.getSchoolId(),
                event.getUser(),
                Type.ASSESSMENT,
                "New grade released!",
                message.toString(),
                event.getMeta(),
                Priority.HIGH);
    }

    @Async
    @EventListener
    public void handleSupplyCollected(SupplyCollectedEvent event) {
        String message = String.format(
                "%s collected %d %s%s.",
                event.getStudentName(),
                event.getQty(),
                event.getMaterialName(),
                event.isFullyCollected() ? "" : " (partial - more still to come)");

        createNotificationUseCase.execute(
                event.getSchoolId(),
                event.getUser(),
                Type.SUPPLY,
                "Material collected",
                message,
                event.getMeta(),
                Priority.NORMAL);
    }
}