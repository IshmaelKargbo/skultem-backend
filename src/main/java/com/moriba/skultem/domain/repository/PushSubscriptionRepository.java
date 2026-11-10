package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.PushSubscription;

public interface PushSubscriptionRepository {
    void save(PushSubscription subscription);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByUserId(String userId);

    void deleteByEndpoint(String endpoint);
}
