package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.AttendanceLocationSetting;

public interface AttendanceLocationSettingRepository {
    void save(AttendanceLocationSetting domain);

    Optional<AttendanceLocationSetting> findBySchoolId(String schoolId);
}
