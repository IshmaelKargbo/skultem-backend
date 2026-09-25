package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceLocationSettingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AttendanceLocationSettingMapper;
import com.moriba.skultem.domain.model.AttendanceLocationSetting;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveAttendanceLocationSettingUseCase {
    private final AttendanceLocationSettingRepository repo;
    private final SchoolRepository schoolRepo;
    private final ManagementSectionRepository sectionRepo;

    // The school-wide location.
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

    // One management section's own location, for a school run from several places.
    public AttendanceLocationSettingDTO executeForSection(String schoolId, String sectionId, double latitude,
            double longitude, int radiusMeters, String allowedIps) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        if (school.getManagementModel() != ManagementModel.SECTION_BASED) {
            throw new RuleException("This school isn't managed in sections");
        }
        if (sectionRepo.findBySchoolId(schoolId).stream().noneMatch(s -> s.getId().equals(sectionId))) {
            throw new NotFoundException("Management section not found");
        }

        var existing = repo.findBySection(schoolId, sectionId).orElse(null);

        AttendanceLocationSetting setting;
        if (existing != null) {
            existing.update(latitude, longitude, radiusMeters, allowedIps);
            setting = existing;
        } else {
            setting = AttendanceLocationSetting.createForSection(UUID.randomUUID().toString(), schoolId, sectionId,
                    latitude, longitude, radiusMeters, allowedIps);
        }

        repo.save(setting);
        return AttendanceLocationSettingMapper.toDTO(setting);
    }
}
