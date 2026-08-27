package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.IdCardSetting;
import com.moriba.skultem.domain.repository.IdCardSettingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.IdCardSettingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.IdCardSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class IdCardSettingAdapter implements IdCardSettingRepository {
    private final IdCardSettingJpaRepository repo;

    @Override
    public void save(IdCardSetting domain) {
        repo.save(IdCardSettingMapper.toEntity(domain));
    }

    @Override
    public Optional<IdCardSetting> findBySchoolId(String schoolId) {
        return repo.findBySchoolId(schoolId).map(IdCardSettingMapper::toDomain);
    }
}
