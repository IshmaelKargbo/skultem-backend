package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.PlatformFeeSetting;

public interface PlatformFeeSettingRepository {
    void save(PlatformFeeSetting domain);

    Optional<PlatformFeeSetting> find();
}
