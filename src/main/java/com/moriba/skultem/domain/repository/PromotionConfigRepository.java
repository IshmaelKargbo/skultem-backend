package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.PromotionConfig;

public interface PromotionConfigRepository {
    void save(PromotionConfig domain);

    Optional<PromotionConfig> findBySchoolId(String schoolId);
}
