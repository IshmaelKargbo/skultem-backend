package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.HealthCareSetting;

public interface HealthCareSettingRepository {

    void save(HealthCareSetting domain);

    Optional<HealthCareSetting> findBySchool(String schoolId);
}
