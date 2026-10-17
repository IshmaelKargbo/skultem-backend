package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.TestimonialEntity;

public interface TestimonialJpaRepository extends JpaRepository<TestimonialEntity, UUID> {
    List<TestimonialEntity> findAllByPublishedTrueOrderByFeaturedDescDisplayOrderAscCreatedAtDesc();
}
