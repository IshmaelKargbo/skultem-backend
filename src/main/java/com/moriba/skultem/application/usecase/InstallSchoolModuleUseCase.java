package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolModuleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.ModuleAccessService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SchoolModuleRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.FeatureModule;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Installs a module for a school, first installing any module it requires. Idempotent: installing
 * something already installed changes nothing. Returns the refreshed catalog so the caller can
 * redraw the Modules page (and see which requirements came along) without a second request.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class InstallSchoolModuleUseCase {

    private final SchoolModuleRepository repo;
    private final ModuleAccessService moduleAccess;
    private final ListSchoolModulesUseCase listSchoolModulesUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MODULE_INSTALLED")
    public List<SchoolModuleDTO> execute(String schoolId, String moduleKey, String userId) {
        var module = FeatureModule.fromKey(moduleKey)
                .orElseThrow(() -> new NotFoundException("Module not found: " + moduleKey));

        install(schoolId, module, userId);
        moduleAccess.evict(schoolId);

        return listSchoolModulesUseCase.execute(schoolId);
    }

    /** Requirements first, so a module is never enabled without what it depends on. */
    private void install(String schoolId, FeatureModule module, String userId) {
        for (var required : module.requires()) {
            install(schoolId, required, userId);
        }

        if (moduleAccess.enabledModules(schoolId).contains(module)) {
            return;
        }

        repo.setEnabled(schoolId, module, true, userId);
        // The cached set is now stale - drop it so the next requirement/dependent check sees this.
        moduleAccess.evict(schoolId);

        logActivityUseCase.log(schoolId, ActivityType.SCHOOL, "Module installed", module.label(), null,
                module.key());
    }
}
