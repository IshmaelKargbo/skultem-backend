package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TimetableSettingDTO;
import com.moriba.skultem.application.mapper.TimetableSettingMapper;
import com.moriba.skultem.domain.repository.TimetableSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetTimetableSettingUseCase {
    private final TimetableSettingRepository repo;

    // No saved design yet: the defaults reproduce the timetable PDF as it looked before this was
    // configurable (landscape, everything shown, school brand colour).
    public TimetableSettingDTO execute(String schoolId) {
        return repo.findBySchoolId(schoolId)
                .map(TimetableSettingMapper::toDTO)
                .orElseGet(() -> new TimetableSettingDTO(TimetableSettingDTO.DEFAULT_TITLE, null,
                        TimetableSettingDTO.LANDSCAPE, true, true, true, true, true, ""));
    }
}
