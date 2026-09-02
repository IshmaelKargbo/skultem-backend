package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PayslipSettingDTO;
import com.moriba.skultem.application.mapper.PayslipSettingMapper;
import com.moriba.skultem.domain.model.PayslipSetting;
import com.moriba.skultem.domain.repository.PayslipSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SavePayslipSettingUseCase {
    private final PayslipSettingRepository repo;

    public PayslipSettingDTO execute(String schoolId, PayslipSettingDTO param) {
        var existing = repo.findBySchoolId(schoolId).orElse(null);

        PayslipSetting setting;
        if (existing != null) {
            existing.update(param.accentColor(), param.logoUrl(), param.footerNote(), param.showWatermark(),
                    param.showAmountInWords());
            setting = existing;
        } else {
            setting = PayslipSetting.create(UUID.randomUUID().toString(), schoolId, param.accentColor(),
                    param.logoUrl(), param.footerNote(), param.showWatermark(), param.showAmountInWords());
        }

        repo.save(setting);
        return PayslipSettingMapper.toDTO(setting);
    }
}
