package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

// One device (browser or installed app) that agreed to native notifications. The endpoint is the push service
// address for that device; p256dh and auth are the device's keys used to encrypt what is sent to it.
@Getter
public class PushSubscription {
    private final String id;
    private final String userId;
    private String schoolId;
    private final String endpoint;
    private String p256dh;
    private String auth;
    private String userAgent;
    private final Instant createdAt;
    private Instant lastUsedAt;

    public PushSubscription(String id, String userId, String schoolId, String endpoint, String p256dh, String auth,
            String userAgent, Instant createdAt, Instant lastUsedAt) {
        this.id = id;
        this.userId = userId;
        this.schoolId = schoolId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
    }

    public static PushSubscription create(String userId, String schoolId, String endpoint, String p256dh, String auth,
            String userAgent) {
        return new PushSubscription(UUID.randomUUID().toString(), userId, schoolId, endpoint, p256dh, auth, userAgent,
                Instant.now(), null);
    }

    // The same device subscribing again (new keys, or now signed in at another school).
    public void refresh(String schoolId, String p256dh, String auth, String userAgent) {
        this.schoolId = schoolId;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
    }

    public void markUsed() {
        this.lastUsedAt = Instant.now();
    }
}
