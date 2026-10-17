package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.PlatformAnnouncementEntity;

public interface PlatformAnnouncementJpaRepository extends JpaRepository<PlatformAnnouncementEntity, UUID> {

    // Switched on and inside its window, newest first (the service puts the most serious on top).
    @Query("""
            select a from PlatformAnnouncementEntity a
            where a.active = true
              and (a.startsAt is null or a.startsAt <= :now)
              and (a.endsAt is null or a.endsAt > :now)
            order by a.createdAt desc
            """)
    List<PlatformAnnouncementEntity> findLive(@Param("now") Instant now);
}
