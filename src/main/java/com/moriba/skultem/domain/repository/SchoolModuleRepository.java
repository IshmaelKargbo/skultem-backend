package com.moriba.skultem.domain.repository;

import java.util.Set;

import com.moriba.skultem.domain.vo.FeatureModule;

public interface SchoolModuleRepository {

    /** The optional modules this school currently has switched on. */
    Set<FeatureModule> findEnabledBySchool(String schoolId);

    /**
     * Turns a module on or off for a school, creating its row on first install. Turning one off
     * keeps the row (and so the history of who installed it) - the data itself is never touched.
     */
    void setEnabled(String schoolId, FeatureModule module, boolean enabled, String changedByUserId);
}
