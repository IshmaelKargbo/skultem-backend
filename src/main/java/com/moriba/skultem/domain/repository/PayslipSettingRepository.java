package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.PayslipSetting;

public interface PayslipSettingRepository {
    void save(PayslipSetting domain);

    Optional<PayslipSetting> findBySchoolId(String schoolId);
}
