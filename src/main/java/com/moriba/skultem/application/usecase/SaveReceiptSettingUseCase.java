package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReceiptSettingDTO;
import com.moriba.skultem.application.mapper.ReceiptSettingMapper;
import com.moriba.skultem.domain.model.ReceiptSetting;
import com.moriba.skultem.domain.repository.ReceiptSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveReceiptSettingUseCase {
    private final ReceiptSettingRepository repo;

    public ReceiptSettingDTO execute(String schoolId, ReceiptSettingDTO param) {
        var existing = repo.findBySchoolId(schoolId).orElse(null);

        ReceiptSetting setting;
        if (existing != null) {
            existing.update(param.accentColor(), param.logoUrl(), param.footerNote(), param.showWatermark(),
                    param.showAmountInWords());
            setting = existing;
        } else {
            setting = ReceiptSetting.create(UUID.randomUUID().toString(), schoolId, param.accentColor(),
                    param.logoUrl(), param.footerNote(), param.showWatermark(), param.showAmountInWords());
        }

        repo.save(setting);
        return ReceiptSettingMapper.toDTO(setting);
    }
}
