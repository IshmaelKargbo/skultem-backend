package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

// One install of the Skultem mobile app that agreed to notifications. The token is Firebase Cloud Messaging's
// address for it; it changes now and then, and the app registers the new one.
@Getter
public class MobileDevice {
    public enum Platform {
        ANDROID, IOS
    }

    private final String id;
    private final String userId;
    private String schoolId;
    private final String token;
    private Platform platform;
    private final Instant createdAt;
    private Instant lastUsedAt;

    public MobileDevice(String id, String userId, String schoolId, String token, Platform platform,
            Instant createdAt, Instant lastUsedAt) {
        this.id = id;
        this.userId = userId;
        this.schoolId = schoolId;
        this.token = token;
        this.platform = platform;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
    }

    public static MobileDevice create(String userId, String schoolId, String token, Platform platform) {
        return new MobileDevice(UUID.randomUUID().toString(), userId, schoolId, token, platform, Instant.now(), null);
    }

    // The same install registering again (e.g. now signed in at another school).
    public void refresh(String schoolId, Platform platform) {
        this.schoolId = schoolId;
        this.platform = platform;
    }

    public void markUsed() {
        this.lastUsedAt = Instant.now();
    }
}
