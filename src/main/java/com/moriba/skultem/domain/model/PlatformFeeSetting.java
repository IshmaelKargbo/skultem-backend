package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * One row per school controlling the platform fee that school's students are charged - see
 * SeedPlatformFeeForAcademicYearUseCase. The id IS the school's id (one setting per school, no
 * separate schoolId column needed) - before this became per-school, a single row shared by every
 * school lived under the fixed id "platform-fee" (see V32__platform_fee_per_school.sql). Set only
 * through a SYSTEM_ADMIN-only endpoint; no school admin/accountant can read or change it via
 * anything reachable from a school's own screens.
 */
@Getter
public class PlatformFeeSetting extends AggregateRoot<String> {

    private BigDecimal amount;

    public PlatformFeeSetting(String id, BigDecimal amount, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.amount = amount;
        touch(updatedAt);
    }

    public static PlatformFeeSetting create(String schoolId, BigDecimal amount) {
        Instant now = Instant.now();
        return new PlatformFeeSetting(schoolId, amount, now, now);
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
        touch(Instant.now());
    }

    public boolean isConfigured() {
        return amount != null && amount.signum() > 0;
    }
}
