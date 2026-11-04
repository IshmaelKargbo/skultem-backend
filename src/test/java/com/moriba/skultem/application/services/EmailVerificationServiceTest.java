package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.EmailVerificationToken;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.EmailVerificationTokenRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.infrastructure.mail.MailService;

class EmailVerificationServiceTest {

    // Tokens kept in memory, so the test sees exactly what the service stored.
    static class Tokens implements EmailVerificationTokenRepository {
        final List<EmailVerificationToken> rows = new ArrayList<>();

        public void save(EmailVerificationToken t) {
            rows.removeIf(r -> r.getId().equals(t.getId()));
            rows.add(t);
        }

        public Optional<EmailVerificationToken> findByTokenHash(String h) {
            return rows.stream().filter(r -> r.getTokenHash().equals(h)).findFirst();
        }

        public List<EmailVerificationToken> findActiveByUserId(String u) {
            return rows.stream().filter(r -> r.getUserId().equals(u) && !r.isUsed() && !r.isRevoked()).toList();
        }

        public long countByUserIdSince(String u, Instant since) {
            return rows.stream().filter(r -> r.getUserId().equals(u) && r.getCreatedAt().isAfter(since)).count();
        }

        public Optional<EmailVerificationToken> findLatestByUserId(String u) {
            return rows.stream().filter(r -> r.getUserId().equals(u))
                    .max(Comparator.comparing(EmailVerificationToken::getCreatedAt));
        }
    }

    private final Tokens tokens = new Tokens();
    private final UserRepository users = mock(UserRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final MailService mail = mock(MailService.class);
    private final EmailValidationService validation = new EmailValidationService(
            d -> MailDomainLookup.Result.ACCEPTS_MAIL, true, 30);
    private EmailVerificationService service;
    private User user;
    private Instant now = Instant.parse("2026-11-03T08:00:00Z");

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(users, tokens, schools, mail, validation, 30, 60, 5,
                "https://{domain}.skultem.space", "https://admin.skultem.space");
        setTime(now);
        user = User.create("Amina", "Kamara", "amina@example.com", "hash", "hint");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(schools.findById(any())).thenReturn(Optional.empty());
    }

    private void setTime(Instant at) {
        now = at;
        service.useClock(Clock.fixed(at, ZoneOffset.UTC));
    }

    private String sentToken() {
        var link = ArgumentCaptor.forClass(String.class);
        verify(mail, org.mockito.Mockito.atLeastOnce()).sendVerificationEmail(eq("amina@example.com"), any(),
                link.capture(), anyLong(), any());
        var last = link.getAllValues().get(link.getAllValues().size() - 1);
        return last.substring(last.indexOf("token=") + "token=".length());
    }

    @Test
    void existingAccountsStartUnverified() {
        assertThat(service.status(user.getId()).status()).isEqualTo("UNVERIFIED");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void sendStoresOnlyAHashAndAnExpiry() {
        var status = service.send(user.getId(), null);
        String token = sentToken();

        assertThat(status.status()).isEqualTo("PENDING");
        assertThat(tokens.rows).hasSize(1);
        var row = tokens.rows.get(0);
        assertThat(row.getTokenHash()).isNotEqualTo(token).isEqualTo(EmailVerificationService.hash(token));
        assertThat(row.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
        assertThat(token).hasSizeGreaterThanOrEqualTo(40);
    }

    @Test
    void openingTheLinkVerifiesTheEmail() {
        service.send(user.getId(), null);
        var result = service.confirm(sentToken());

        assertThat(result.status()).isEqualTo("VERIFIED");
        assertThat(result.email()).isEqualTo("a***@example.com");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(now);
        assertThat(tokens.rows.get(0).isUsed()).isTrue();
        assertThat(service.status(user.getId()).status()).isEqualTo("VERIFIED");
    }

    @Test
    void aLinkWorksOnlyOnce() {
        service.send(user.getId(), null);
        String token = sentToken();
        service.confirm(token);
        assertThat(service.confirm(token).status()).isEqualTo("ALREADY_VERIFIED");
    }

    @Test
    void expiredLinksAreRefused() {
        service.send(user.getId(), null);
        String token = sentToken();
        setTime(now.plus(Duration.ofMinutes(31)));

        assertThat(service.confirm(token).status()).isEqualTo("EXPIRED");
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(service.status(user.getId()).status()).isEqualTo("EXPIRED");
    }

    @Test
    void unknownTokensAreInvalid() {
        assertThat(service.confirm("not-a-real-token").status()).isEqualTo("INVALID");
        assertThat(service.confirm("").status()).isEqualTo("INVALID");
        assertThat(service.confirm(null).status()).isEqualTo("INVALID");
    }

    @Test
    void resendingReplacesTheEarlierLink() {
        service.send(user.getId(), null);
        String first = sentToken();
        setTime(now.plus(Duration.ofMinutes(2)));
        service.send(user.getId(), null);
        String second = sentToken();

        assertThat(second).isNotEqualTo(first);
        assertThat(service.confirm(first).status()).isEqualTo("SUPERSEDED");
        assertThat(service.confirm(second).status()).isEqualTo("VERIFIED");
    }

    @Test
    void resendingIsRateLimited() {
        service.send(user.getId(), null);
        assertThatThrownBy(() -> service.send(user.getId(), null))
                .isInstanceOf(RuleException.class).hasMessageContaining("seconds");

        for (int i = 1; i < 5; i++) {
            setTime(now.plus(Duration.ofMinutes(2)));
            service.send(user.getId(), null);
        }
        setTime(now.plus(Duration.ofMinutes(2)));
        assertThatThrownBy(() -> service.send(user.getId(), null))
                .isInstanceOf(RuleException.class).hasMessageContaining("hour");
    }

    @Test
    void aLinkForAnOldAddressDoesNotVerifyTheNewOne() {
        service.send(user.getId(), null);
        String token = sentToken();
        user.changeEmail("amina.new@example.com");

        assertThat(service.confirm(token).status()).isEqualTo("EMAIL_CHANGED");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void changingAVerifiedEmailResetsVerification() {
        service.send(user.getId(), null);
        service.confirm(sentToken());
        assertThat(user.isEmailVerified()).isTrue();

        user.changeEmail("Amina@Example.com"); // only the case changed - same address
        assertThat(user.isEmailVerified()).isTrue();

        user.changeEmail("other@example.com");
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    void cannotSendForAnAccountWithoutEmailOrAlreadyVerified() {
        var noEmail = User.create("No", "Email", null, "hash", "hint");
        when(users.findById(noEmail.getId())).thenReturn(Optional.of(noEmail));
        assertThatThrownBy(() -> service.send(noEmail.getId(), null)).isInstanceOf(RuleException.class);
        assertThat(service.status(noEmail.getId()).status()).isEqualTo("NO_EMAIL");

        service.send(user.getId(), null);
        service.confirm(sentToken());
        setTime(now.plus(Duration.ofMinutes(5)));
        assertThatThrownBy(() -> service.send(user.getId(), null)).hasMessageContaining("already verified");
    }

    @Test
    void anAddressOnADomainThatDoesNotExistGetsNoLink() {
        var strict = new EmailVerificationService(users, tokens, schools, mail,
                new EmailValidationService(d -> MailDomainLookup.Result.NO_SUCH_DOMAIN, true, 30), 30, 60, 5,
                "https://{domain}.skultem.space", "https://admin.skultem.space");
        assertThatThrownBy(() -> strict.send(user.getId(), null)).isInstanceOf(RuleException.class);
        assertThat(tokens.rows).isEmpty();
    }

    @Test
    void aFailedSendIsReportedWithoutTheLink() {
        doThrow(new RuntimeException("smtp down")).when(mail)
                .sendVerificationEmail(anyString(), any(), anyString(), anyLong(), any());
        assertThatThrownBy(() -> service.send(user.getId(), null))
                .isInstanceOf(RuleException.class)
                .hasMessageNotContaining("token");
    }

    @Test
    void theLinkPointsAtTheSchoolsPortal() {
        var school = mock(com.moriba.skultem.domain.model.School.class);
        when(school.getDomain()).thenReturn("moriba");
        when(school.getName()).thenReturn("Moriba School");
        when(schools.findById("SCL-1")).thenReturn(Optional.of(school));

        service.send(user.getId(), "SCL-1");
        var link = ArgumentCaptor.forClass(String.class);
        verify(mail).sendVerificationEmail(eq("amina@example.com"), eq("Amina"), link.capture(), eq(30L),
                eq("Moriba School"));
        assertThat(link.getValue()).startsWith("https://moriba.skultem.space/verify-email?token=");
    }
}
