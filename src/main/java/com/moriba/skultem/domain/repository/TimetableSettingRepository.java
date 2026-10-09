package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.TimetableSetting;

public interface TimetableSettingRepository {
    void save(TimetableSetting domain);

    Optional<TimetableSetting> findBySchoolId(String schoolId);
}
