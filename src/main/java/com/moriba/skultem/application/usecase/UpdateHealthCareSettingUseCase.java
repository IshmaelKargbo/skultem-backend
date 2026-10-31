package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareSettingDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.repository.HealthCareSettingRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Saves a school's health care rules (§6). Principal-level only (P-3). Rules apply to visits started afterwards
 * (S-3). A complaint the school added can't be removed once a visit used it - it can be hidden instead (C-3).
 * Settings aren't medical, so the audit row keeps the full old -> new detail (S-2).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateHealthCareSettingUseCase {
    private final HealthCareSettingRepository settingRepo;
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "HEALTH_CARE_SETTINGS_UPDATED")
    public HealthCareSettingDTO execute(String schoolId, HealthCareSetting.Values values, String userId) {
        var setting = settingRepo.findBySchool(schoolId).orElseGet(() -> HealthCareSetting.defaults(schoolId));
        var before = setting.customCodes();

        LibraryAttendanceSupport.rule(() -> setting.update(values, userId, support.userName(userId), support.now()));

        var after = setting.customCodes();
        for (String code : before) {
            if (!after.contains(code) && visitRepo.existsWithComplaint(schoolId, code)) {
                throw new RuleException("A complaint you removed is used by past visits. Hide it instead of removing it.");
            }
        }
        settingRepo.save(setting);
        logActivityUseCase.log(schoolId, ActivityType.HEALTH_CARE, "Health care settings updated", null, null, null);
        return HealthCareDtoMapper.toDTO(setting, true);
    }
}
