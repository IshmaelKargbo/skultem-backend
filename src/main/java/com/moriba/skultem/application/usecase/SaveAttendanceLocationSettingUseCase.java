package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceLocationSettingDTO;
import com.moriba.skultem.application.mapper.AttendanceLocationSettingMapper;
import com.moriba.skultem.domain.model.AttendanceLocationSetting;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveAttendanceLocationSettingUseCase {
    private final AttendanceLocationSettingRepository repo;

    public AttendanceLocationSettingDTO execute(String schoolId, double latitude, double longitude,
            int radiusMeters, String allowedIps) {
        var existing = repo.findBySchoolId(schoolId).orElse(null);

        AttendanceLocationSetting setting;
        if (existing != null) {
            existing.update(latitude, longitude, radiusMeters, allowedIps);
            setting = existing;
        } else {
            setting = AttendanceLocationSetting.create(UUID.randomUUID().toString(), schoolId, latitude, longitude,
                    radiusMeters, allowedIps);
        }

        repo.save(setting);
        return AttendanceLocationSettingMapper.toDTO(setting);
    }
}
