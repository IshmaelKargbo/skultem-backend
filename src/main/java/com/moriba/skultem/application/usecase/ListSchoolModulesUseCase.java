package com.moriba.skultem.application.usecase;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolModuleDTO;
import com.moriba.skultem.application.services.ModuleAccessService;
import com.moriba.skultem.domain.vo.FeatureModule;

import lombok.RequiredArgsConstructor;

/** The full optional-module catalog, each marked installed or not for the given school. */
@Service
@RequiredArgsConstructor
public class ListSchoolModulesUseCase {

    private final ModuleAccessService moduleAccess;

    public List<SchoolModuleDTO> execute(String schoolId) {
        var installed = moduleAccess.enabledModules(schoolId);

        return Arrays.stream(FeatureModule.values())
                .map(module -> new SchoolModuleDTO(
                        module.key(),
                        module.label(),
                        module.description(),
                        module.category().name().toLowerCase(),
                        module.category().label(),
                        installed.contains(module),
                        module.starter(),
                        module.requires().stream().map(FeatureModule::key).toList(),
                        module.requiredBy().stream().map(FeatureModule::key).toList()))
                .toList();
    }
}
