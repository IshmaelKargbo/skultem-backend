package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AttendanceLocationSetting;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AttendanceLocationSettingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AttendanceLocationSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendanceLocationSettingAdapter implements AttendanceLocationSettingRepository {
    private final AttendanceLocationSettingJpaRepository repo;

    @Override
    public void save(AttendanceLocationSetting domain) {
        repo.save(AttendanceLocationSettingMapper.toEntity(domain));
    }

    @Override
    public Optional<AttendanceLocationSetting> findBySchoolId(String schoolId) {
        return repo.findBySchoolId(schoolId).map(AttendanceLocationSettingMapper::toDomain);
    }
}
