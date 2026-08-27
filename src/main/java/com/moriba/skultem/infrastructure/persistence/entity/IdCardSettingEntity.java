package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "id_card_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdCardSettingEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    @Column(nullable = false)
    private String layout;

    @Column(nullable = false)
    private String profileShape;

    @Column(nullable = false)
    private String headerColor;

    @Column(nullable = false)
    private String footerColor;

    @Column(nullable = false)
    private String headerTextColor;

    @Column(nullable = false)
    private String primaryTextColor;

    @Column(nullable = false)
    private int widthMm;

    @Column(nullable = false)
    private int heightMm;

    private String bgImageUrl;

    @Column(nullable = false)
    private int bgOpacity;

    private String schoolName;
    private String schoolAddress;
    private String principalName;

    @Column(nullable = false, columnDefinition = "text")
    private String fields;

    @Column(nullable = false)
    private int validityYears;

    private Instant createdAt;
    private Instant updatedAt;
}
