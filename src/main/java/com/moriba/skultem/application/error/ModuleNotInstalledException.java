package com.moriba.skultem.application.error;

import com.moriba.skultem.domain.vo.FeatureModule;

/** A request reached a feature the caller's school hasn't installed - see FeatureModule. */
public class ModuleNotInstalledException extends RuntimeException {

    private final FeatureModule module;

    public ModuleNotInstalledException(FeatureModule module) {
        super(module.label() + " isn't installed for this school. Install it from the Modules page.");
        this.module = module;
    }

    public FeatureModule getModule() {
        return module;
    }
}
