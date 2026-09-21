package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.services.ModuleAccessService;
import com.moriba.skultem.domain.repository.SchoolModuleRepository;
import com.moriba.skultem.domain.vo.FeatureModule;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Gives a brand-new school its starter modules (see {@link FeatureModule#starterModules()}) - the
 * important ones it needs on day one; everything else it installs later, as it needs it.
 * Idempotent: a starter module the school already has is left as it is, including one it has
 * deliberately disabled.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProvisionStarterModulesForNewSchoolUseCase {

    private final SchoolModuleRepository repo;
    private final ModuleAccessService moduleAccess;

    public void execute(String schoolId, String createdByUserId) {
        for (var module : FeatureModule.starterModules()) {
            repo.setEnabled(schoolId, module, true, createdByUserId);
        }
        moduleAccess.evict(schoolId);
    }
}
