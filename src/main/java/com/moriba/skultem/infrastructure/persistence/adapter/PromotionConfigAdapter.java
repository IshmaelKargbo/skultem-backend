package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PromotionConfig;
import com.moriba.skultem.domain.repository.PromotionConfigRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.PromotionConfigJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.PromotionConfigMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PromotionConfigAdapter implements PromotionConfigRepository {
    private final PromotionConfigJpaRepository repo;

    @Override
    public void save(PromotionConfig domain) {
        repo.save(PromotionConfigMapper.toEntity(domain));
    }

    @Override
    public Optional<PromotionConfig> findBySchoolId(String schoolId) {
        return repo.findBySchoolId(schoolId).map(PromotionConfigMapper::toDomain);
    }
}
