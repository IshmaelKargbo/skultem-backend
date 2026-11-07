package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PlatformFeeSetting;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;
import com.moriba.skultem.infrastructure.persistence.entity.PlatformFeeSettingEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.PlatformFeeSettingJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlatformFeeSettingAdapter implements PlatformFeeSettingRepository {
    private final PlatformFeeSettingJpaRepository repo;

    @Override
    public void save(PlatformFeeSetting domain) {
        var entity = PlatformFeeSettingEntity.builder()
                .id(domain.getId())
                .amount(domain.getAmount())
                .installmentMode(domain.getInstallmentMode())
                .termShares(PlatformFeeSetting.formatShares(domain.getTermShares()))
                .updatedAt(domain.getUpdatedAt())
                .build();
        repo.save(entity);
    }

    @Override
    public Optional<PlatformFeeSetting> findBySchool(String schoolId) {
        return repo.findById(schoolId)
                .map(PlatformFeeSettingAdapter::toDomain);
    }

    @Override
    public List<PlatformFeeSetting> findAll() {
        return repo.findAll().stream()
                .map(PlatformFeeSettingAdapter::toDomain)
                .toList();
    }

    private static PlatformFeeSetting toDomain(com.moriba.skultem.infrastructure.persistence.entity.PlatformFeeSettingEntity e) {
        return new PlatformFeeSetting(e.getId(), e.getAmount(), e.getInstallmentMode(),
                PlatformFeeSetting.parseShares(e.getTermShares()), e.getUpdatedAt(), e.getUpdatedAt());
    }
}
