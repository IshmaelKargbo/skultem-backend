package com.moriba.skultem.infrastructure.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.moriba.skultem.application.events.GradesReleasedEvent;
import com.moriba.skultem.application.usecase.CreateNotificationUseCase;
import com.moriba.skultem.domain.model.Notification.Type;
import com.moriba.skultem.domain.vo.Priority;

@Component
public class GradesReleasedNotificationListener {

    private final CreateNotificationUseCase createNotificationUseCase;

    public GradesReleasedNotificationListener(
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
        
        System.out.println(message);
        createNotificationUseCase.execute(
                event.getSchoolId(),
                event.getUser(),
                Type.ASSESSMENT,
                "New grade released!",
                message.toString(),
                event.getMeta(),
                Priority.HIGH);
    }
}