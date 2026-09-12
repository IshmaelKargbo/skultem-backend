package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.PlatformFeeSetting;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Ensures a school has a {@link PlatformFeeSetting} row, creating one if it doesn't - see
 * {@link SeedPlatformFeeForAcademicYearUseCase} for why a missing row is otherwise a silent
 * "no platform fee" rather than an error. A school that already has a row (configured amount or
 * not) is left untouched here - this never overwrites a SYSTEM_ADMIN's deliberate choice,
 * including a deliberate zero/blank amount.
 * <p>
 * The amount used for a newly-created row is copied from whichever amount is most common among
 * already-configured schools - platform fee started as one global amount before
 * {@code V32__platform_fee_per_school.sql} split it per school, so "the amount everyone else
 * already has" is the closest thing to a real default. If no school anywhere has one configured
 * yet, there's nothing to copy and this is a no-op.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class EnsurePlatformFeeSettingUseCase {

    private final PlatformFeeSettingRepository settingRepo;

    public void execute(String schoolId) {
        if (settingRepo.findBySchool(schoolId).isPresent()) {
            return;
        }

        BigDecimal defaultAmount = resolveDefaultAmount();
        if (defaultAmount == null) {
            return;
        }

        settingRepo.save(PlatformFeeSetting.create(schoolId, defaultAmount));
    }

    private BigDecimal resolveDefaultAmount() {
        List<BigDecimal> configuredAmounts = settingRepo.findAll().stream()
                .filter(PlatformFeeSetting::isConfigured)
                .map(PlatformFeeSetting::getAmount)
                .toList();

        if (configuredAmounts.isEmpty()) {
            return null;
        }

        // Mode, not just the first hit - a single school's one-off override shouldn't become
        // every new school's default over whatever amount the rest of the platform actually uses.
        return configuredAmounts.stream()
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
