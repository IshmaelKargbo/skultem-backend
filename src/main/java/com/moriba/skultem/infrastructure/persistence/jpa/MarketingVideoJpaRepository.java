package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.MarketingVideoEntity;

public interface MarketingVideoJpaRepository extends JpaRepository<MarketingVideoEntity, UUID> {
    List<MarketingVideoEntity> findAllByPublishedTrueOrderByFeaturedDescDisplayOrderAscCreatedAtDesc();

    boolean existsByYoutubeIdAndIdNot(String youtubeId, UUID id);
}
