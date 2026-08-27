package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionConfigDTO;
import com.moriba.skultem.domain.model.PromotionConfig;
import com.moriba.skultem.domain.repository.PromotionConfigRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetPromotionConfigUseCase {

    private final PromotionConfigRepository promotionConfigRepo;

    public PromotionConfigDTO execute(String schoolId) {
        return toDTO(resolve(schoolId));
    }

    /**
     * Returns the school's config, creating a default row the first time it's read so callers never
     * have to null-check.
     */
    public PromotionConfig resolve(String schoolId) {
        return promotionConfigRepo.findBySchoolId(schoolId)
                .orElseGet(() -> {
                    var config = PromotionConfig.createDefault(UUID.randomUUID().toString(), schoolId);
                    promotionConfigRepo.save(config);
                    return config;
                });
    }

    private PromotionConfigDTO toDTO(PromotionConfig config) {
        return new PromotionConfigDTO(config.getMinPassMark(), config.getMaxRepeatCount(),
                config.isRequireApproval(), config.isRequireRemarkForPromote());
    }
}
