package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PlatformFeeSettingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.PlatformFeeSetting;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * SYSTEM_ADMIN-only: sets the amount a specific school's platform fee is seeded with (see
 * SeedPlatformFeeForAcademicYearUseCase) - each school has its own, independent amount. Changing
 * it only affects platform fees seeded after the change - it never rewrites amounts already
 * charged to students. Not logged to that school's own activity feed (this isn't something the
 * school did) - the {@code @AuditLogAnnotation} still records who made the change in the system
 * audit log.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdatePlatformFeeSettingUseCase {
    private final PlatformFeeSettingRepository repo;
    private final SchoolRepository schoolRepo;

    @AuditLogAnnotation(action = "PLATFORM_FEE_SETTING_UPDATED")
    public PlatformFeeSettingDTO execute(String schoolId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new RuleException("Platform fee amount must be greater than zero");
        }

        schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));

        var setting = repo.findBySchool(schoolId).orElseGet(() -> PlatformFeeSetting.create(schoolId, amount));
        setting.updateAmount(amount);
        repo.save(setting);

        return new PlatformFeeSettingDTO(schoolId, setting.getAmount(), setting.getUpdatedAt());
    }
}
