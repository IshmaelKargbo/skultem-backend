package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class UserSession extends AggregateRoot<String> {

    private final String schoolId;
    private final User user;
    private final String ipAddress;
    private final String device;
    private final String deviceType;
    private final String os;
    private final String browser;
    private final String userAgent;
    private boolean active;

    public UserSession(String id, String schoolId, User user, String ipAddress, String device, String deviceType,
            String os, String browser, String userAgent, boolean active, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.user = user;
        this.schoolId = schoolId;
        this.deviceType = deviceType;
        this.device = device;
        this.browser = browser;
        this.os = os;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.active = active;
        touch(updatedAt);
    }

    public static UserSession create(String id, String schoolId, User user, String ipAddress,
            String device, String deviceType, String os, String browser, String userAgent) {
        Instant now = Instant.now();
        return new UserSession(id, schoolId, user, ipAddress, device, deviceType, os, browser,
                userAgent, true, now, now);
    }

    public void deactivate(){
        this.active = false;
    }
}
