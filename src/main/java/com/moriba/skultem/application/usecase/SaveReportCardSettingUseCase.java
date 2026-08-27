package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.mapper.ReportCardSettingMapper;
import com.moriba.skultem.domain.model.ReportCardSetting;
import com.moriba.skultem.domain.repository.ReportCardSettingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveReportCardSettingUseCase {
    private final ReportCardSettingRepository repo;

    public ReportCardSettingDTO execute(String schoolId, ReportCardSettingDTO param) {
        var existing = repo.findBySchoolId(schoolId).orElse(null);

        ReportCardSetting setting;
        if (existing != null) {
            existing.update(param.headerColor(), param.logoUrl(), param.footerNote(), param.showAttendance(),
                    param.showRemarks(), param.showPosition(), param.showSignatures(), param.showGradeScale());
            setting = existing;
        } else {
            setting = ReportCardSetting.create(UUID.randomUUID().toString(), schoolId, param.headerColor(),
                    param.logoUrl(), param.footerNote(), param.showAttendance(), param.showRemarks(),
                    param.showPosition(), param.showSignatures(), param.showGradeScale());
        }

        repo.save(setting);
        return ReportCardSettingMapper.toDTO(setting);
    }
}
