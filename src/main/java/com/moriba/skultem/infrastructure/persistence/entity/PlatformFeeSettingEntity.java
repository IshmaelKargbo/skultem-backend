package com.moriba.skultem.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import com.moriba.skultem.domain.model.PlatformFeeSetting;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformFeeSettingEntity {
    @Id
    private String id;

    @Column(name = "platform_fee_amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "installment_mode", nullable = false, length = 16)
    @Builder.Default
    private PlatformFeeSetting.InstallmentMode installmentMode = PlatformFeeSetting.InstallmentMode.BY_TERM;

    // "50,30,20" percent per term; null = equal parts.
    @Column(name = "term_shares", length = 100)
    private String termShares;

    private Instant updatedAt;
}
