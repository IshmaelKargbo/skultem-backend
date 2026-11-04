package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.PasswordResetToken;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.PasswordResetTokenRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.mail.MailService;

class PasswordResetServiceTest {

    // Tokens kept in memory, so the test sees exactly what the service stored.
    static class Tokens implements PasswordResetTokenRepository {
        final List<PasswordResetToken> rows = new ArrayList<>();

        public void save(PasswordResetToken t) {
            rows.removeIf(r -> r.getId().equals(t.getId()));
            rows.add(t);
        }

        public Optional<PasswordResetToken> findByTokenHash(String h) {
            return rows.stream().filter(r -> r.getTokenHash().equals(h)).findFirst();
        }

        public List<PasswordResetToken> findActiveByUserId(String u) {
            return rows.stream()
                    .filter(r -> r.getUserId().equals(u) && r.getUsedAt() == null && r.getRevokedAt() == null)
                    .toList();
        }

        public long countByUserIdSince(String u, Instant since) {
            return rows.stream().filter(r -> r.getUserId().equals(u) && r.getCreatedAt().isAfter(since)).count();
        }

        public Optional<PasswordResetToken> findLatestByUserId(String u) {
            return rows.stream().filter(r -> r.getUserId().equals(u))
                    .max(Comparator.comparing(PasswordResetToken::getCreatedAt));
        }
    }

    private final Tokens tokens = new Tokens();
    private final UserRepository users = mock(UserRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final SchoolUserRepository memberships = mock(SchoolUserRepository.class);
    private final UserSessionRepository sessions = mock(UserSessionRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final MailService mail = mock(MailService.class);
    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(users, schools, memberships, sessions, tokens, encoder, mail, 30, 60, 5,
                "https://{domain}.skultem.space");
        setTime(Instant.parse("2026-11-04T08:00:00Z"));

        user = User.create("Amina", "Kamara", "amina@example.com", "old-hash", "hint");
        var school = mock(School.class);
        when(school.getId()).thenReturn("SCL-1");
        when(school.getDomain()).thenReturn("kwis");
        when(schools.findByDomain("kwis")).thenReturn(Optional.of(school));
        when(users.findByEmail("amina@example.com")).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var member = SchoolUser.create("SCL-1", user, Role.TEACHER);
        when(memberships.findAllByUser_IdAndSchoolId(user.getId(), "SCL-1")).thenReturn(List.of(member));
        when(memberships.findAllByUser_Id(user.getId())).thenReturn(List.of(member));
        when(encoder.encode(anyString())).thenAnswer(i -> "hashed:" + i.getArgument(0));
    }

    private void setTime(Instant at) {
        service.useClock(Clock.fixed(at, ZoneOffset.UTC));
    }

    private String sentLink() {
        var link = ArgumentCaptor.forClass(String.class);
        verify(mail, org.mockito.Mockito.atLeastOnce()).sendPasswordResetEmail(eq("amina@example.com"),
                link.capture());
        return link.getAllValues().get(link.getAllValues().size() - 1);
    }

    private String sentToken() {
        var link = sentLink();
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    @Test
    void requestEmailsALinkToTheSchoolsOwnSite() {
        service.request("kwis", "  amina@example.com ");

        assertThat(sentLink()).startsWith("https://kwis.skultem.space/set-password?token=");
        assertThat(tokens.rows).hasSize(1);
        assertThat(tokens.rows.get(0).getTokenHash()).isEqualTo(EmailVerificationService.hash(sentToken()));
    }

    @Test
    void unknownEmailOrSchoolSendsNothingAndSaysNothing() {
        service.request("kwis", "nobody@example.com");
        service.request("no-such-school", "amina@example.com");

        verify(mail, never()).sendPasswordResetEmail(any(), any());
        assertThat(tokens.rows).isEmpty();
    }

    @Test
    void notAMemberOfThatSchoolSendsNothing() {
        when(memberships.findAllByUser_IdAndSchoolId(user.getId(), "SCL-1")).thenReturn(List.of());

        service.request("kwis", "amina@example.com");

        verify(mail, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void secondRequestWithinTheCooldownIsIgnored() {
        service.request("kwis", "amina@example.com");
        service.request("kwis", "amina@example.com");

        verify(mail, times(1)).sendPasswordResetEmail(any(), any());
    }

    @Test
    void aFailedEmailDoesNotBreakTheRequest() {
        doThrow(new RuntimeException("Template email failed")).when(mail).sendPasswordResetEmail(any(), any());

        service.request("kwis", "amina@example.com");

        assertThat(tokens.rows).hasSize(1);
    }

    @Test
    void theLinkSetsTheNewPasswordAndSignsOutEverywhere() {
        var session = mock(UserSession.class);
        when(sessions.findAllByUserAndSchoolIdAndActive(user.getId(), "SCL-1", true)).thenReturn(List.of(session));
        service.request("kwis", "amina@example.com");

        service.reset(sentToken(), "newpass123");

        assertThat(user.getPassword()).isEqualTo("hashed:newpass123");
        verify(session).deactivate();
        verify(sessions).save(session);
        assertThat(tokens.rows.get(0).getUsedAt()).isNotNull();
    }

    @Test
    void aLinkWorksOnlyOnce() {
        service.request("kwis", "amina@example.com");
        String token = sentToken();
        service.reset(token, "newpass123");

        assertThatThrownBy(() -> service.reset(token, "another1"))
                .isInstanceOf(RuleException.class).hasMessage(PasswordResetService.INVALID_LINK);
    }

    @Test
    void anExpiredLinkIsRefused() {
        service.request("kwis", "amina@example.com");
        String token = sentToken();
        setTime(Instant.parse("2026-11-04T08:00:00Z").plus(Duration.ofMinutes(31)));

        assertThatThrownBy(() -> service.reset(token, "newpass123"))
                .isInstanceOf(RuleException.class).hasMessage(PasswordResetService.INVALID_LINK);
        assertThat(user.getPassword()).isEqualTo("old-hash");
    }

    @Test
    void aNewerLinkReplacesTheOlderOne() {
        service.request("kwis", "amina@example.com");
        String first = sentToken();
        setTime(Instant.parse("2026-11-04T08:02:00Z"));
        service.request("kwis", "amina@example.com");
        String second = sentToken();

        assertThatThrownBy(() -> service.reset(first, "newpass123")).isInstanceOf(RuleException.class);
        service.reset(second, "newpass123");
        assertThat(user.getPassword()).isEqualTo("hashed:newpass123");
    }

    @Test
    void madeUpTokensAndShortPasswordsAreRefused() {
        assertThatThrownBy(() -> service.reset("not-a-real-token", "newpass123"))
                .isInstanceOf(RuleException.class).hasMessage(PasswordResetService.INVALID_LINK);
        assertThatThrownBy(() -> service.reset("anything", "12345"))
                .isInstanceOf(RuleException.class);
    }
}
