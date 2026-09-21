package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.PlatformFeeSetting;
import com.moriba.skultem.domain.repository.FeeCategoryRepository;
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
 * <p>
 * A school that is brand new is different - see {@link #provisionForNewSchool(String)}, which
 * always starts it at {@link PlatformFeeSetting#DEFAULT_AMOUNT} with its "Platform Fee" fee
 * category already in place, instead of depending on what other schools happen to have.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class EnsurePlatformFeeSettingUseCase {

    private final PlatformFeeSettingRepository settingRepo;
    private final FeeCategoryRepository feeCategoryRepo;

    /**
     * Called once when a school is created: gives it a platform fee setting at
     * {@link PlatformFeeSetting#DEFAULT_AMOUNT} and creates its "Platform Fee" fee category. The
     * fee structure itself still can't exist until the school has a term (see
     * {@link SeedPlatformFeeForAcademicYearUseCase}), which finds this same category by name
     * rather than creating a second one - so once the first term activates, every enrolled student
     * is charged, and every student admitted after that picks it up at enrollment time.
     * Idempotent: an existing setting/category is left untouched.
     */
    public void provisionForNewSchool(String schoolId) {
        if (settingRepo.findBySchool(schoolId).isEmpty()) {
            settingRepo.save(PlatformFeeSetting.create(schoolId, PlatformFeeSetting.DEFAULT_AMOUNT));
        }

        var categoryName = SeedPlatformFeeForAcademicYearUseCase.PLATFORM_FEE_CATEGORY_NAME;
        if (feeCategoryRepo.findByNameAndSchool(categoryName, schoolId).isEmpty()) {
            feeCategoryRepo.save(FeeCategory.create(UUID.randomUUID().toString(), schoolId, categoryName,
                    "Platform subscription fee"));
        }
    }

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
