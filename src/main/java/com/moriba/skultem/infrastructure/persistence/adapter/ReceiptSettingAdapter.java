package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ReceiptSetting;
import com.moriba.skultem.domain.repository.ReceiptSettingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ReceiptSettingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ReceiptSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReceiptSettingAdapter implements ReceiptSettingRepository {
    private final ReceiptSettingJpaRepository repo;

    @Override
    public void save(ReceiptSetting domain) {
        repo.save(ReceiptSettingMapper.toEntity(domain));
    }

    @Override
    public Optional<ReceiptSetting> findBySchoolId(String schoolId) {
        return repo.findBySchoolId(schoolId).map(ReceiptSettingMapper::toDomain);
    }
}
