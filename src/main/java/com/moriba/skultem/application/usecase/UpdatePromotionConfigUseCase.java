package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PromotionConfigDTO;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.PromotionConfigRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdatePromotionConfigUseCase {

    private final PromotionConfigRepository promotionConfigRepo;
    private final GetPromotionConfigUseCase getPromotionConfigUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PROMOTION_CONFIG_UPDATED")
    public PromotionConfigDTO execute(String schoolId, Integer minPassMark, int maxRepeatCount,
            boolean requireApproval, boolean requireRemarkForPromote) {
        var config = getPromotionConfigUseCase.resolve(schoolId);
        config.update(minPassMark, maxRepeatCount, requireApproval, requireRemarkForPromote);
        promotionConfigRepo.save(config);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SETTINGS,
                "Promotion rules updated",
                "Max repeats: " + maxRepeatCount + ", approval required: " + requireApproval,
                null,
                config.getId());

        return new PromotionConfigDTO(config.getMinPassMark(), config.getMaxRepeatCount(),
                config.isRequireApproval(), config.isRequireRemarkForPromote());
    }
}
