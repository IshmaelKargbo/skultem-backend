package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.MobileDevice;
import com.moriba.skultem.domain.repository.MobileDeviceRepository;
import com.moriba.skultem.infrastructure.persistence.entity.MobileDeviceEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.MobileDeviceJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MobileDeviceAdapter implements MobileDeviceRepository {
    private final MobileDeviceJpaRepository repo;

    @Override
    public void save(MobileDevice d) {
        repo.save(MobileDeviceEntity.builder().id(d.getId()).userId(d.getUserId()).schoolId(d.getSchoolId())
                .token(d.getToken()).platform(d.getPlatform()).createdAt(d.getCreatedAt())
                .lastUsedAt(d.getLastUsedAt()).build());
    }

    @Override
    public Optional<MobileDevice> findByToken(String token) {
        return repo.findByToken(token).map(MobileDeviceAdapter::toDomain);
    }

    @Override
    public List<MobileDevice> findAllByUserId(String userId) {
        return repo.findAllByUserId(userId).stream().map(MobileDeviceAdapter::toDomain).toList();
    }

    @Override
    public void deleteByToken(String token) {
        repo.deleteByToken(token);
    }

    private static MobileDevice toDomain(MobileDeviceEntity e) {
        return new MobileDevice(e.getId(), e.getUserId(), e.getSchoolId(), e.getToken(), e.getPlatform(),
                e.getCreatedAt(), e.getLastUsedAt());
    }
}
