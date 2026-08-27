package com.moriba.skultem.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * The one global row controlling the platform fee every school's students are charged - see
 * SeedPlatformFeeForAcademicYearUseCase. Set only through a SYSTEM_ADMIN-only endpoint; no school
 * admin/accountant can read or change it via anything reachable from a school's own screens.
 */
@Getter
public class PlatformFeeSetting extends AggregateRoot<String> {
    public static final String ID = "platform-fee";

    private BigDecimal amount;

    public PlatformFeeSetting(String id, BigDecimal amount, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.amount = amount;
        touch(updatedAt);
    }

    public static PlatformFeeSetting create(BigDecimal amount) {
        Instant now = Instant.now();
        return new PlatformFeeSetting(ID, amount, now, now);
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
        touch(Instant.now());
    }

    public boolean isConfigured() {
        return amount != null && amount.signum() > 0;
    }
}
