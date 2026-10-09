package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TimetableSettingDTO;
import com.moriba.skultem.application.mapper.TimetableSettingMapper;
import com.moriba.skultem.domain.model.TimetableSetting;
import com.moriba.skultem.domain.repository.TimetableSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveTimetableSettingUseCase {
    private final TimetableSettingRepository repo;

    public TimetableSettingDTO execute(String schoolId, TimetableSettingDTO param) {
        String title = param.title() == null || param.title().isBlank() ? TimetableSettingDTO.DEFAULT_TITLE
                : param.title().trim();
        String accent = param.accentColor() == null || param.accentColor().isBlank() ? null : param.accentColor().trim();
        String orientation = TimetableSettingDTO.PORTRAIT.equalsIgnoreCase(param.orientation())
                ? TimetableSettingDTO.PORTRAIT
                : TimetableSettingDTO.LANDSCAPE;

        var existing = repo.findBySchoolId(schoolId).orElse(null);

        TimetableSetting setting;
        if (existing != null) {
            existing.update(title, accent, orientation, param.showLogo(), param.showIcons(), param.showTeacher(),
                    param.showRoom(), param.showPeriodTimes(), param.footerNote());
            setting = existing;
        } else {
            setting = TimetableSetting.create(UUID.randomUUID().toString(), schoolId, title, accent, orientation,
                    param.showLogo(), param.showIcons(), param.showTeacher(), param.showRoom(),
                    param.showPeriodTimes(), param.footerNote());
        }

        repo.save(setting);
        return TimetableSettingMapper.toDTO(setting);
    }
}
