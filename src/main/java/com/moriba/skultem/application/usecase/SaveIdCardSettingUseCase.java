package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.IdCardSettingDTO;
import com.moriba.skultem.application.mapper.IdCardSettingMapper;
import com.moriba.skultem.domain.model.IdCardSetting;
import com.moriba.skultem.domain.repository.IdCardSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveIdCardSettingUseCase {
    private final IdCardSettingRepository repo;

    public IdCardSettingDTO execute(String schoolId, IdCardSettingDTO param) {
        var existing = repo.findBySchoolId(schoolId).orElse(null);

        IdCardSetting setting;
        if (existing != null) {
            existing.update(param.layout(), param.profileShape(), param.headerColor(), param.footerColor(),
                    param.headerTextColor(), param.primaryTextColor(), param.widthMm(), param.heightMm(),
                    param.bgImageUrl(), param.bgOpacity(), param.schoolName(), param.schoolAddress(),
                    param.principalName(), param.fields(), param.validityYears());
            setting = existing;
        } else {
            setting = IdCardSetting.create(UUID.randomUUID().toString(), schoolId, param.layout(),
                    param.profileShape(), param.headerColor(), param.footerColor(), param.headerTextColor(),
                    param.primaryTextColor(), param.widthMm(), param.heightMm(), param.bgImageUrl(),
                    param.bgOpacity(), param.schoolName(), param.schoolAddress(), param.principalName(),
                    param.fields(), param.validityYears());
        }

        repo.save(setting);
        return IdCardSettingMapper.toDTO(setting);
    }
}
