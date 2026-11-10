package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.infrastructure.persistence.entity.PushSubscriptionEntity;

public interface PushSubscriptionJpaRepository extends JpaRepository<PushSubscriptionEntity, String> {
    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    List<PushSubscriptionEntity> findAllByUserId(String userId);

    @Transactional
    void deleteByEndpoint(String endpoint);
}
