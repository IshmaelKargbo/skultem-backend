package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PlatformFeeSettingDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.PlatformFeeSetting;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * SYSTEM_ADMIN-only: sets the amount every school's platform fee is seeded with (see
 * SeedPlatformFeeForAcademicYearUseCase). Changing it only affects platform fees seeded after the
 * change - it never rewrites amounts already charged to students. Not logged to any school's
 * activity feed (this isn't scoped to a school) - the {@code @AuditLogAnnotation} still records who
 * made the change in the system audit log.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdatePlatformFeeSettingUseCase {
    private final PlatformFeeSettingRepository repo;

    @AuditLogAnnotation(action = "PLATFORM_FEE_SETTING_UPDATED")
    public PlatformFeeSettingDTO execute(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new RuleException("Platform fee amount must be greater than zero");
        }

        var setting = repo.find().orElseGet(() -> PlatformFeeSetting.create(amount));
        setting.updateAmount(amount);
        repo.save(setting);

        return new PlatformFeeSettingDTO(setting.getAmount(), setting.getUpdatedAt());
    }
}
