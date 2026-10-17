package com.moriba.skultem.application.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.infrastructure.persistence.entity.PlatformAnnouncementEntity;
import com.moriba.skultem.infrastructure.persistence.entity.PlatformAnnouncementEntity.Severity;
import com.moriba.skultem.infrastructure.persistence.jpa.PlatformAnnouncementJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SavePlatformAnnouncementDTO;

class PlatformAnnouncementServiceTest {

    private static PlatformAnnouncementEntity announcement(boolean active, Instant starts, Instant ends) {
        return PlatformAnnouncementEntity.builder().id(UUID.randomUUID()).title("t").message("m")
                .severity(Severity.INFO).active(active).startsAt(starts).endsAt(ends).build();
    }

    @Test
    void liveMeansSwitchedOnAndInsideItsWindow() {
        var now = Instant.now();
        assertTrue(PlatformAnnouncementService.isLive(announcement(true, null, null), now));
        assertTrue(PlatformAnnouncementService.isLive(
                announcement(true, now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(1))), now));
        assertFalse(PlatformAnnouncementService.isLive(announcement(false, null, null), now));
        assertFalse(PlatformAnnouncementService.isLive(announcement(true, now.plus(Duration.ofHours(1)), null), now));
        assertFalse(PlatformAnnouncementService.isLive(announcement(true, null, now.minus(Duration.ofHours(1))), now));
    }

    @Test
    void anEndBeforeTheStartIsRefused() {
        var repo = mock(PlatformAnnouncementJpaRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new PlatformAnnouncementService(repo);
        var now = Instant.now();
        var bad = new SavePlatformAnnouncementDTO("Maintenance", "Down tonight", "WARNING", true, now, now.minusSeconds(60));
        assertThatThrownBy(() -> service.create(bad, "u1")).isInstanceOf(RuleException.class);
    }
}
