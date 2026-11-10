package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PushSubscription;
import com.moriba.skultem.domain.repository.PushSubscriptionRepository;
import com.moriba.skultem.infrastructure.persistence.entity.PushSubscriptionEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.PushSubscriptionJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PushSubscriptionAdapter implements PushSubscriptionRepository {
    private final PushSubscriptionJpaRepository repo;

    @Override
    public void save(PushSubscription s) {
        repo.save(PushSubscriptionEntity.builder().id(s.getId()).userId(s.getUserId()).schoolId(s.getSchoolId())
                .endpoint(s.getEndpoint()).p256dh(s.getP256dh()).auth(s.getAuth()).userAgent(s.getUserAgent())
                .createdAt(s.getCreatedAt()).lastUsedAt(s.getLastUsedAt()).build());
    }

    @Override
    public Optional<PushSubscription> findByEndpoint(String endpoint) {
        return repo.findByEndpoint(endpoint).map(PushSubscriptionAdapter::toDomain);
    }

    @Override
    public List<PushSubscription> findAllByUserId(String userId) {
        return repo.findAllByUserId(userId).stream().map(PushSubscriptionAdapter::toDomain).toList();
    }

    @Override
    public void deleteByEndpoint(String endpoint) {
        repo.deleteByEndpoint(endpoint);
    }

    private static PushSubscription toDomain(PushSubscriptionEntity e) {
        return new PushSubscription(e.getId(), e.getUserId(), e.getSchoolId(), e.getEndpoint(), e.getP256dh(),
                e.getAuth(), e.getUserAgent(), e.getCreatedAt(), e.getLastUsedAt());
    }
}
