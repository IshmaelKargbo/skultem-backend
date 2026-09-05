package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PlatformFeeSettingDTO;
import com.moriba.skultem.domain.repository.PlatformFeeSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPlatformFeeSettingUseCase {
    private final PlatformFeeSettingRepository repo;

    public PlatformFeeSettingDTO execute(String schoolId) {
        return repo.findBySchool(schoolId)
                .map(s -> new PlatformFeeSettingDTO(schoolId, s.getAmount(), s.getUpdatedAt()))
                .orElse(new PlatformFeeSettingDTO(schoolId, null, null));
    }
}
