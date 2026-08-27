package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.IdCardSetting;

public interface IdCardSettingRepository {
    void save(IdCardSetting domain);

    Optional<IdCardSetting> findBySchoolId(String schoolId);
}
