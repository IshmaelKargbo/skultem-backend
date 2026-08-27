package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.ReportCardSetting;

public interface ReportCardSettingRepository {
    void save(ReportCardSetting domain);

    Optional<ReportCardSetting> findBySchoolId(String schoolId);
}
