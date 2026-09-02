package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceLocationSettingDTO;
import com.moriba.skultem.application.mapper.AttendanceLocationSettingMapper;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAttendanceLocationSettingUseCase {
    private final AttendanceLocationSettingRepository repo;

    public AttendanceLocationSettingDTO execute(String schoolId) {
        return AttendanceLocationSettingMapper.toDTO(repo.findBySchoolId(schoolId).orElse(null));
    }
}
