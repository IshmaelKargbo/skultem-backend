package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.AttendanceLocationSetting;

public interface AttendanceLocationSettingRepository {
    void save(AttendanceLocationSetting domain);

    // The school-wide location (no management section).
    Optional<AttendanceLocationSetting> findBySchoolId(String schoolId);

    Optional<AttendanceLocationSetting> findBySection(String schoolId, String managementSectionId);

    // Every configured location: the school-wide one and each section's own.
    List<AttendanceLocationSetting> findAllBySchoolId(String schoolId);
}
