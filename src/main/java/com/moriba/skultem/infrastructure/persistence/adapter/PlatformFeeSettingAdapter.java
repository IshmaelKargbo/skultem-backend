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
                .updatedAt(domain.getUpdatedAt())
                .build();
        repo.save(entity);
    }

    @Override
    public Optional<PlatformFeeSetting> findBySchool(String schoolId) {
        return repo.findById(schoolId)
                .map(e -> new PlatformFeeSetting(e.getId(), e.getAmount(), e.getUpdatedAt(), e.getUpdatedAt()));
    }

    @Override
    public List<PlatformFeeSetting> findAll() {
        return repo.findAll().stream()
                .map(e -> new PlatformFeeSetting(e.getId(), e.getAmount(), e.getUpdatedAt(), e.getUpdatedAt()))
                .toList();
    }
}
