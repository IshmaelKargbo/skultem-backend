package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.ReceiptSetting;

public interface ReceiptSettingRepository {
    void save(ReceiptSetting domain);

    Optional<ReceiptSetting> findBySchoolId(String schoolId);
}
