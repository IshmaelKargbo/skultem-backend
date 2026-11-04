package com.moriba.skultem.application.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.EmailVerificationResultDTO;
import com.moriba.skultem.application.dto.EmailVerificationStatusDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.EmailVerificationToken;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.EmailVerificationTokenRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.infrastructure.mail.MailService;

import lombok.extern.slf4j.Slf4j;

// Proves a person owns their account's email: a link with a random token goes to the address, and opening it marks
// the address verified. Shared by every kind of account (staff, parents, owners, platform admins) - they all keep their
// email on the user account.
//   - The token is 32 random bytes; only its SHA-256 is stored. It's never logged.
//   - A link works once, until it expires (email-verification.token-ttl-minutes, default 30), and only for the address
//     it was sent to - a later change of email makes it useless.
//   - Sending a new link revokes the earlier ones.
//   - Sending is limited per account: a cooldown between links and a cap per hour.
@Slf4j
@Service
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final EmailVerificationTokenRepository tokenRepo;
    private final SchoolRepository schoolRepo;
    private final MailService mailService;
    private final EmailValidationService emailValidation;
    private final Duration ttl;
    private final Duration cooldown;
    private final int maxPerHour;
    private final String portalUrl;
    private final String adminPortalUrl;
    private Clock clock = Clock.systemUTC();

    @Autowired
    public EmailVerificationService(UserRepository userRepo, EmailVerificationTokenRepository tokenRepo,
            SchoolRepository schoolRepo, MailService mailService, EmailValidationService emailValidation,
            @Value("${email-verification.token-ttl-minutes:30}") long ttlMinutes,
            @Value("${email-verification.resend-cooldown-seconds:60}") long cooldownSeconds,
            @Value("${email-verification.max-per-hour:5}") int maxPerHour,
            @Value("${app.portal-url:https://{domain}.skultem.space}") String portalUrl,
            @Value("${app.admin-portal-url:https://admin.skultem.space}") String adminPortalUrl) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.schoolRepo = schoolRepo;
        this.mailService = mailService;
        this.emailValidation = emailValidation;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.cooldown = Duration.ofSeconds(cooldownSeconds);
        this.maxPerHour = maxPerHour;
        this.portalUrl = portalUrl;
        this.adminPortalUrl = adminPortalUrl;
    }

    // Tests pin the time.
    void useClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Sends a verification link to the account's current email. schoolId is the school the request comes from (its
     * address goes in the link); null on the admin portal.
     */
    @Transactional
    public EmailVerificationStatusDTO send(String userId, String schoolId) {
        var user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Account not found"));
        var email = EmailValidationService.normalize(user.getEmail());
        if (email == null) {
            throw new RuleException("This account has no email address to verify");
        }
        if (user.isEmailVerified()) {
            throw new RuleException("This email address is already verified");
        }
        // A clearly unusable address (no such domain) would only bounce.
        emailValidation.requireAcceptable(email);

        Instant now = clock.instant();
        enforceLimits(userId, now);

        for (var previous : tokenRepo.findActiveByUserId(userId)) {
            previous.revoke(now);
            tokenRepo.save(previous);
        }

        String token = newToken();
        var record = EmailVerificationToken.issue(userId, email, hash(token), schoolId, now, now.plus(ttl));
        tokenRepo.save(record);

        String schoolName = null;
        String base = adminPortalUrl;
        if (schoolId != null) {
            var school = schoolRepo.findById(schoolId).orElse(null);
            if (school != null) {
                schoolName = school.getName();
                base = portalUrl.replace("{domain}", school.getDomain());
            }
        }
        String link = trimSlash(base) + "/verify-email?token=" + token;
        try {
            mailService.sendVerificationEmail(email, user.getGivenNames(), link, ttl.toMinutes(), schoolName);
        } catch (RuntimeException e) {
            // Never the token or link in the log - only that sending failed.
            log.warn("Verification email to user {} could not be sent: {}", userId, e.getMessage());
            throw new RuleException("The verification email couldn't be sent. Please try again in a few minutes.");
        }
        return status(user, now);
    }

    /** Opens a verification link. Public - the token itself is the proof. */
    @Transactional
    public EmailVerificationResultDTO confirm(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            return new EmailVerificationResultDTO("INVALID", null);
        }
        String hashed = hash(token.trim());
        var found = tokenRepo.findByTokenHash(hashed);
        // Looked up by hash; compared again in constant time so timing says nothing about near-matches.
        if (found.isEmpty() || !MessageDigest.isEqual(found.get().getTokenHash().getBytes(StandardCharsets.UTF_8),
                hashed.getBytes(StandardCharsets.UTF_8))) {
            return new EmailVerificationResultDTO("INVALID", null);
        }
        var record = found.get();
        String masked = mask(record.getEmail());
        Instant now = clock.instant();

        var user = userRepo.findById(record.getUserId()).orElse(null);
        if (user == null) {
            return new EmailVerificationResultDTO("INVALID", null);
        }
        boolean sameAddress = user.getEmail() != null && user.getEmail().equalsIgnoreCase(record.getEmail());
        if (record.isUsed()) {
            return new EmailVerificationResultDTO(sameAddress && user.isEmailVerified() ? "ALREADY_VERIFIED" : "INVALID",
                    masked);
        }
        if (record.isRevoked()) {
            return new EmailVerificationResultDTO("SUPERSEDED", masked);
        }
        if (record.isExpired(now)) {
            return new EmailVerificationResultDTO("EXPIRED", masked);
        }
        if (!sameAddress) {
            record.revoke(now);
            tokenRepo.save(record);
            return new EmailVerificationResultDTO("EMAIL_CHANGED", masked);
        }

        record.markUsed(now);
        tokenRepo.save(record);
        if (!user.isEmailVerified()) {
            user.markEmailVerified(record.getEmail(), now);
            userRepo.save(user);
        }
        return new EmailVerificationResultDTO("VERIFIED", masked);
    }

    @Transactional(readOnly = true)
    public EmailVerificationStatusDTO status(String userId) {
        var user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Account not found"));
        return status(user, clock.instant());
    }

    private EmailVerificationStatusDTO status(User user, Instant now) {
        var email = EmailValidationService.normalize(user.getEmail());
        if (email == null) {
            return new EmailVerificationStatusDTO(null, "NO_EMAIL", null, null, null);
        }
        if (user.isEmailVerified()) {
            return new EmailVerificationStatusDTO(email, "VERIFIED", user.getEmailVerifiedAt(), null, null);
        }
        var latest = tokenRepo.findLatestByUserId(user.getId())
                .filter(t -> t.getEmail().equalsIgnoreCase(email));
        Instant resendAt = latest.map(t -> t.getCreatedAt().plus(cooldown)).filter(t -> t.isAfter(now)).orElse(null);
        if (latest.isPresent() && !latest.get().isUsed() && !latest.get().isRevoked()) {
            var t = latest.get();
            return t.isExpired(now)
                    ? new EmailVerificationStatusDTO(email, "EXPIRED", null, null, resendAt)
                    : new EmailVerificationStatusDTO(email, "PENDING", null, t.getExpiresAt(), resendAt);
        }
        return new EmailVerificationStatusDTO(email, "UNVERIFIED", null, null, resendAt);
    }

    private void enforceLimits(String userId, Instant now) {
        var latest = tokenRepo.findLatestByUserId(userId);
        if (latest.isPresent() && latest.get().getCreatedAt().plus(cooldown).isAfter(now)) {
            long wait = Duration.between(now, latest.get().getCreatedAt().plus(cooldown)).toSeconds() + 1;
            throw new RuleException("A verification email was just sent. You can send another in " + wait + " seconds.");
        }
        if (tokenRepo.countByUserIdSince(userId, now.minus(Duration.ofHours(1))) >= maxPerHour) {
            throw new RuleException("Too many verification emails for this account. Try again in an hour.");
        }
    }

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
