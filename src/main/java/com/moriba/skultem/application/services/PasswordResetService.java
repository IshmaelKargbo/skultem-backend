package com.moriba.skultem.application.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.PasswordResetToken;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.PasswordResetTokenRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.infrastructure.mail.MailService;

import lombok.extern.slf4j.Slf4j;

// Self-service "forgot password" on a school's own site:
//   - request(): if the email belongs to an active member of that school, a link with a random token is emailed to it.
//     The caller always gets the same answer, so the form can't be used to find out who has an account.
//   - reset(): the link's token sets a new password, signs the account out everywhere, and the link stops working.
// The token is 32 random bytes; only its SHA-256 is stored and it's never logged. A link works once, until it expires
// (password-reset.token-ttl-minutes, default 30). A new link replaces earlier ones. Sending is limited per account.
@Slf4j
@Service
public class PasswordResetService {

    static final String INVALID_LINK = "This reset link is invalid or has expired. Please request a new one.";

    private final UserRepository userRepo;
    private final SchoolRepository schoolRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final UserSessionRepository sessionRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Duration ttl;
    private final Duration cooldown;
    private final int maxPerHour;
    private final String portalUrl;
    private Clock clock = Clock.systemUTC();

    @Autowired
    public PasswordResetService(UserRepository userRepo, SchoolRepository schoolRepo,
            SchoolUserRepository schoolUserRepo, UserSessionRepository sessionRepo,
            PasswordResetTokenRepository tokenRepo, PasswordEncoder passwordEncoder, MailService mailService,
            @Value("${password-reset.token-ttl-minutes:30}") long ttlMinutes,
            @Value("${password-reset.resend-cooldown-seconds:60}") long cooldownSeconds,
            @Value("${password-reset.max-per-hour:5}") int maxPerHour,
            @Value("${app.portal-url:https://{domain}.skultem.space}") String portalUrl) {
        this.userRepo = userRepo;
        this.schoolRepo = schoolRepo;
        this.schoolUserRepo = schoolUserRepo;
        this.sessionRepo = sessionRepo;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.cooldown = Duration.ofSeconds(cooldownSeconds);
        this.maxPerHour = maxPerHour;
        this.portalUrl = portalUrl;
    }

    // Tests pin the time.
    void useClock(Clock clock) {
        this.clock = clock;
    }

    /** Emails a reset link if `email` is an active member of the school at `domain`. Silent otherwise. */
    @Transactional
    public void request(String domain, String email) {
        String normalized = EmailValidationService.normalize(email);
        if (normalized == null || domain == null || domain.isBlank()) {
            return;
        }
        var school = schoolRepo.findByDomain(domain.trim()).orElse(null);
        if (school == null) {
            return;
        }
        var user = userRepo.findByEmail(normalized).orElse(null);
        if (user == null || !canReset(user, school.getId())) {
            return;
        }

        Instant now = clock.instant();
        var latest = tokenRepo.findLatestByUserId(user.getId());
        if (latest.isPresent() && latest.get().getCreatedAt().plus(cooldown).isAfter(now)) {
            return;
        }
        if (tokenRepo.countByUserIdSince(user.getId(), now.minus(Duration.ofHours(1))) >= maxPerHour) {
            return;
        }

        revokeActive(user.getId(), now);
        String token = EmailVerificationService.newToken();
        tokenRepo.save(PasswordResetToken.issue(user.getId(), EmailVerificationService.hash(token), school.getId(),
                now, now.plus(ttl)));

        String base = portalUrl.replace("{domain}", school.getDomain());
        String link = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                + "/set-password?token=" + token;
        try {
            mailService.sendPasswordResetEmail(user.getEmail(), link);
        } catch (RuntimeException e) {
            // Never the token or link in the log - only that sending failed.
            log.error("Password reset email to user {} could not be sent: {}", user.getId(), e.getMessage());
        }
    }

    /** Sets a new password from a reset link. Public - the token itself is the proof. */
    @Transactional
    public void reset(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuleException("Password must be between 6 and 20 characters");
        }
        if (token == null || token.isBlank() || token.length() > 200) {
            throw new RuleException(INVALID_LINK);
        }
        String hashed = EmailVerificationService.hash(token.trim());
        var found = tokenRepo.findByTokenHash(hashed);
        // Looked up by hash; compared again in constant time so timing says nothing about near-matches.
        if (found.isEmpty() || !MessageDigest.isEqual(found.get().getTokenHash().getBytes(StandardCharsets.UTF_8),
                hashed.getBytes(StandardCharsets.UTF_8))) {
            throw new RuleException(INVALID_LINK);
        }
        var record = found.get();
        Instant now = clock.instant();
        if (!record.isUsable(now)) {
            throw new RuleException(INVALID_LINK);
        }
        var user = userRepo.findById(record.getUserId()).orElse(null);
        if (user == null || !canReset(user, record.getSchoolId())) {
            throw new RuleException(INVALID_LINK);
        }

        user.setNewPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        record.markUsed(now);
        tokenRepo.save(record);
        revokeActive(user.getId(), now);

        // Whoever knew the old password is signed out, at every school this account belongs to.
        schoolUserRepo.findAllByUser_Id(user.getId()).stream()
                .map(SchoolUser::getSchoolId)
                .filter(id -> id != null)
                .distinct()
                .forEach(schoolId -> sessionRepo.findAllByUserAndSchoolIdAndActive(user.getId(), schoolId, true)
                        .forEach(session -> {
                            session.deactivate();
                            sessionRepo.save(session);
                        }));
    }

    // An account that can sign in at this school: not deactivated, with an active membership there.
    private boolean canReset(User user, String schoolId) {
        if (user.getStatus() == User.Status.INACTIVE || user.getStatus() == User.Status.DELETED) {
            return false;
        }
        return schoolId != null && schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), schoolId).stream()
                .anyMatch(m -> m.getStatus() == SchoolUser.Status.ACTIVE);
    }

    private void revokeActive(String userId, Instant now) {
        for (var previous : tokenRepo.findActiveByUserId(userId)) {
            previous.revoke(now);
            tokenRepo.save(previous);
        }
    }
}
