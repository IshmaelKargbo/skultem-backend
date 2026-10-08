package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.mapper.ReportCardSettingMapper;
import com.moriba.skultem.domain.model.ReportCardSetting;
import com.moriba.skultem.application.services.RemarkScale;
import com.moriba.skultem.domain.repository.ReportCardSettingRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveReportCardSettingUseCase {
    private final ReportCardSettingRepository repo;

    public ReportCardSettingDTO execute(String schoolId, ReportCardSettingDTO param) {
        var remarkScale = RemarkScale.normalize(param.remarkScale());
        var remarkScaleJson = remarkScale.isEmpty() ? null : JsonMapper.toJson(remarkScale);

        var existing = repo.findBySchoolId(schoolId).orElse(null);

        ReportCardSetting setting;
        if (existing != null) {
            existing.update(param.headerColor(), param.logoUrl(), param.footerNote(), param.showAttendance(),
                    param.showRemarks(), param.showPosition(), param.showTeacherSignature(), param.showPrincipalSignature(), param.showGradeScale(),
                    remarkScaleJson);
            setting = existing;
        } else {
            setting = ReportCardSetting.create(UUID.randomUUID().toString(), schoolId, param.headerColor(),
                    param.logoUrl(), param.footerNote(), param.showAttendance(), param.showRemarks(),
                    param.showPosition(), param.showTeacherSignature(), param.showPrincipalSignature(), param.showGradeScale(),
                    remarkScaleJson);
        }

        repo.save(setting);
        return ReportCardSettingMapper.toDTO(setting);
    }
}
