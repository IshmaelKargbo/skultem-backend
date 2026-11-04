package com.moriba.skultem.application.dto;

import java.time.Instant;

// Where an account's email stands. status:
//   NO_EMAIL   - the account has no email;
//   VERIFIED   - the person opened a link sent to this exact address (verifiedAt);
//   PENDING    - a link was sent and is still valid (pendingExpiresAt);
//   EXPIRED    - the last link expired unused;
//   UNVERIFIED - never verified (every account that existed before verification starts here).
// resendAvailableAt: when another link can be sent (null = now).
public record EmailVerificationStatusDTO(String email, String status, Instant verifiedAt, Instant pendingExpiresAt,
        Instant resendAvailableAt) {
}
