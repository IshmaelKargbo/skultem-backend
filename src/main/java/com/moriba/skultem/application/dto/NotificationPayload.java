package com.moriba.skultem.application.dto;

import java.time.Instant;

import lombok.Getter;

@Getter
public class NotificationPayload {
    private final long count;
    private final Instant timestamp;
    private final NotificationDTO notification;

    public NotificationPayload(long count, Instant timestamp) {
        this(count, timestamp, null);
    }

    public NotificationPayload(long count, Instant timestamp, NotificationDTO notification) {
        this.count = count;
        this.timestamp = timestamp;
        this.notification = notification;
    }
}
