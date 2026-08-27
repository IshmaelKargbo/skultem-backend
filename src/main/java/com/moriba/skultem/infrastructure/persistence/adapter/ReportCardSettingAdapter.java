package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ReportCardSetting;
import com.moriba.skultem.domain.repository.ReportCardSettingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ReportCardSettingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ReportCardSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReportCardSettingAdapter implements ReportCardSettingRepository {
    private final ReportCardSettingJpaRepository repo;

    @Override
    public void save(ReportCardSetting domain) {
        repo.save(ReportCardSettingMapper.toEntity(domain));
    }

    @Override
    public Optional<ReportCardSetting> findBySchoolId(String schoolId) {
        return repo.findBySchoolId(schoolId).map(ReportCardSettingMapper::toDomain);
    }
}
