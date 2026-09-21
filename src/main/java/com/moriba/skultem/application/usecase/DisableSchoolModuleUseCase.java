package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolModuleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.ModuleAccessService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SchoolModuleRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.FeatureModule;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Disables a module for a school: its screens and API are switched off, but none of its data is
 * touched, so installing it again brings everything back. Refused while another installed module
 * still requires it (e.g. Grading & Assessments while Report Cards is installed).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DisableSchoolModuleUseCase {

    private final SchoolModuleRepository repo;
    private final ModuleAccessService moduleAccess;
    private final ListSchoolModulesUseCase listSchoolModulesUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MODULE_DISABLED")
    public List<SchoolModuleDTO> execute(String schoolId, String moduleKey, String userId) {
        var module = FeatureModule.fromKey(moduleKey)
                .orElseThrow(() -> new NotFoundException("Module not found: " + moduleKey));

        var installed = moduleAccess.enabledModules(schoolId);
        if (!installed.contains(module)) {
            return listSchoolModulesUseCase.execute(schoolId);
        }

        var stillNeeded = module.requiredBy().stream().filter(installed::contains).map(FeatureModule::label)
                .toList();
        if (!stillNeeded.isEmpty()) {
            throw new RuleException("Disable " + String.join(", ", stillNeeded) + " first - "
                    + (stillNeeded.size() == 1 ? "it needs" : "they need") + " " + module.label() + ".");
        }

        repo.setEnabled(schoolId, module, false, userId);
        moduleAccess.evict(schoolId);

        logActivityUseCase.log(schoolId, ActivityType.SCHOOL, "Module disabled", module.label(), null,
                module.key());

        return listSchoolModulesUseCase.execute(schoolId);
    }
}
