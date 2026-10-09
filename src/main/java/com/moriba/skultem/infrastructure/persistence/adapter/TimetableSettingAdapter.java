package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.TimetableSetting;
import com.moriba.skultem.domain.repository.TimetableSettingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TimetableSettingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TimetableSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TimetableSettingAdapter implements TimetableSettingRepository {
    private final TimetableSettingJpaRepository repo;

    @Override
    public void save(TimetableSetting domain) {
        repo.save(TimetableSettingMapper.toEntity(domain));
    }

    @Override
    public Optional<TimetableSetting> findBySchoolId(String schoolId) {
        return repo.findBySchoolId(schoolId).map(TimetableSettingMapper::toDomain);
    }
}
