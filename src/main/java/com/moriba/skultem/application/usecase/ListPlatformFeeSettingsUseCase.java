package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PlatformFeeSettingDTO;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;

import lombok.RequiredArgsConstructor;

/**
 * Every school's platform fee setting in one call - backs the system-admin schools list, which
 * would otherwise need one GetPlatformFeeSettingUseCase call per school on screen. A school
 * missing from the result simply has no amount configured yet (see PlatformFeeSettingRepository).
 */
@Service
@RequiredArgsConstructor
public class ListPlatformFeeSettingsUseCase {
    private final PlatformFeeSettingRepository repo;

    public List<PlatformFeeSettingDTO> execute() {
        return repo.findAll().stream()
                .map(s -> new PlatformFeeSettingDTO(s.getId(), s.getAmount(), s.getUpdatedAt()))
                .toList();
    }
}
