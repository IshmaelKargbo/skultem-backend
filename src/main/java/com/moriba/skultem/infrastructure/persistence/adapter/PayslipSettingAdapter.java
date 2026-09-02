package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PayslipSetting;
import com.moriba.skultem.domain.repository.PayslipSettingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.PayslipSettingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.PayslipSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PayslipSettingAdapter implements PayslipSettingRepository {
    private final PayslipSettingJpaRepository repo;

    @Override
    public void save(PayslipSetting domain) {
        repo.save(PayslipSettingMapper.toEntity(domain));
    }

    @Override
    public Optional<PayslipSetting> findBySchoolId(String schoolId) {
        return repo.findBySchoolId(schoolId).map(PayslipSettingMapper::toDomain);
    }
}
