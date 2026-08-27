package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Audience;

import lombok.Getter;

@Getter
public class Broadcast extends AggregateRoot<String> {

    private String schoolId;
    private String title;
    private String message;
    private Audience audience;
    private List<Channel> channels;
    private Status status;
    private int recipientsCount;
    private int deliveredCount;
    private String sentByUserId;
    private String sentByName;
    private Instant scheduledAt;
    private Instant sentAt;

    public enum Channel {
        SMS,
        EMAIL,
        PUSH,
        IN_APP
    }

    public enum Status {
        SENT,
        SCHEDULED,
        FAILED
    }

    public enum SendOption {
        NOW,
        SCHEDULE
    }

    public Broadcast(String id, String schoolId, String title, String message, Audience audience,
            List<Channel> channels, Status status, int recipientsCount, int deliveredCount, String sentByUserId,
            String sentByName, Instant scheduledAt, Instant sentAt, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.title = title;
        this.message = message;
        this.audience = audience;
        this.channels = channels;
        this.status = status;
        this.recipientsCount = recipientsCount;
        this.deliveredCount = deliveredCount;
        this.sentByUserId = sentByUserId;
        this.sentByName = sentByName;
        this.scheduledAt = scheduledAt;
        this.sentAt = sentAt;
        touch(updatedAt);
    }

    public static Broadcast compose(String id, String schoolId, String title, String message, Audience audience,
            List<Channel> channels, SendOption sendOption, Instant scheduledAt, int recipientsCount,
            String sentByUserId, String sentByName) {
        Instant now = Instant.now();
        boolean isNow = sendOption == SendOption.NOW;

        Status status = isNow ? Status.SENT : Status.SCHEDULED;
        int deliveredCount = isNow ? (int) Math.round(recipientsCount * (0.93 + Math.random() * 0.06)) : 0;

        return new Broadcast(id, schoolId, title, message, audience, channels, status, recipientsCount,
                deliveredCount, sentByUserId, sentByName, isNow ? null : scheduledAt, isNow ? now : null, now, now);
    }
}
