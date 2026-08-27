package com.moriba.skultem.infrastructure.persistence.adapter;

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
                .updatedAt(domain.getUpdatedAt())
                .build();
        repo.save(entity);
    }

    @Override
    public Optional<PlatformFeeSetting> find() {
        return repo.findById(PlatformFeeSetting.ID)
                .map(e -> new PlatformFeeSetting(e.getId(), e.getAmount(), e.getUpdatedAt(), e.getUpdatedAt()));
    }
}
