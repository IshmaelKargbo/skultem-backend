package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PlatformAnnouncementDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.infrastructure.persistence.entity.PlatformAnnouncementEntity;
import com.moriba.skultem.infrastructure.persistence.entity.PlatformAnnouncementEntity.Severity;
import com.moriba.skultem.infrastructure.persistence.jpa.PlatformAnnouncementJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SavePlatformAnnouncementDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Messages from the system admin that show on every user's notice board, whichever school they belong to.
 * Managed by system admins; read by anyone signed in (only what is live right now).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PlatformAnnouncementService {

    private final PlatformAnnouncementJpaRepository repo;

    public Page<PlatformAnnouncementDTO> list(int page, int size) {
        return repo.findAll(PageableMapper.toPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(e -> toDTO(e, Instant.now()));
    }

    public List<PlatformAnnouncementDTO> listLive() {
        var now = Instant.now();
        // Most serious first (CRITICAL, WARNING, INFO); within a level the newest stays first.
        return repo.findLive(now).stream()
                .sorted(java.util.Comparator.comparing((PlatformAnnouncementEntity e) -> e.getSeverity().ordinal())
                        .reversed())
                .map(e -> toDTO(e, now)).toList();
    }

    public PlatformAnnouncementDTO create(SavePlatformAnnouncementDTO p, String createdBy) {
        var now = Instant.now();
        var entity = PlatformAnnouncementEntity.builder().id(UUID.randomUUID()).createdBy(createdBy).createdAt(now)
                .build();
        apply(entity, p, now);
        return toDTO(repo.save(entity), now);
    }

    public PlatformAnnouncementDTO update(String id, SavePlatformAnnouncementDTO p) {
        var entity = find(id);
        var now = Instant.now();
        apply(entity, p, now);
        return toDTO(repo.save(entity), now);
    }

    public void delete(String id) {
        repo.delete(find(id));
    }

    private PlatformAnnouncementEntity find(String id) {
        return repo.findById(TestimonialService.parse(id))
                .orElseThrow(() -> new NotFoundException("Announcement not found"));
    }

    private void apply(PlatformAnnouncementEntity e, SavePlatformAnnouncementDTO p, Instant now) {
        if (p.startsAt() != null && p.endsAt() != null && !p.endsAt().isAfter(p.startsAt())) {
            throw new RuleException("The end must be after the start");
        }
        e.setTitle(p.title().trim());
        e.setMessage(p.message().trim());
        e.setSeverity(Severity.valueOf(p.severity()));
        e.setActive(p.active());
        e.setStartsAt(p.startsAt());
        e.setEndsAt(p.endsAt());
        e.setUpdatedAt(now);
    }

    static boolean isLive(PlatformAnnouncementEntity e, Instant now) {
        return e.isActive() && (e.getStartsAt() == null || !e.getStartsAt().isAfter(now))
                && (e.getEndsAt() == null || e.getEndsAt().isAfter(now));
    }

    private static PlatformAnnouncementDTO toDTO(PlatformAnnouncementEntity e, Instant now) {
        return new PlatformAnnouncementDTO(e.getId().toString(), e.getTitle(), e.getMessage(), e.getSeverity().name(),
                e.isActive(), e.getStartsAt(), e.getEndsAt(), isLive(e, now), e.getCreatedAt(), e.getUpdatedAt());
    }
}
