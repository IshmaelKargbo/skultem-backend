package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.repository.HealthCareSettingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.HealthCareSettingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.HealthCareMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HealthCareSettingAdapter implements HealthCareSettingRepository {
    private final HealthCareSettingJpaRepository repo;

    @Override
    public void save(HealthCareSetting domain) {
        repo.save(HealthCareMapper.toEntity(domain));
    }

    @Override
    public Optional<HealthCareSetting> findBySchool(String schoolId) {
        return repo.findBySchoolId(schoolId).map(HealthCareMapper::toDomain);
    }
}
