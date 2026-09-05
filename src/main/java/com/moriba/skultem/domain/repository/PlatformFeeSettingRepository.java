package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.PlatformFeeSetting;

public interface PlatformFeeSettingRepository {
    void save(PlatformFeeSetting domain);

    Optional<PlatformFeeSetting> findBySchool(String schoolId);

    // Only schools with a configured amount have a row at all (see
    // SeedPlatformFeeForAcademicYearUseCase's no-op-until-configured behavior) - a school missing
    // from this list simply has none set yet. Used by the system-admin schools list so it isn't
    // one request per school.
    List<PlatformFeeSetting> findAll();
}
