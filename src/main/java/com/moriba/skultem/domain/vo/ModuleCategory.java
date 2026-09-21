package com.moriba.skultem.domain.vo;

/** How the Modules page groups {@link FeatureModule}s so the catalog reads by purpose, not alphabet. */
public enum ModuleCategory {
    ACADEMICS("Academics"),
    FINANCE("Finance"),
    PEOPLE("People & HR"),
    COMMUNICATION("Communication"),
    OPERATIONS("Operations"),
    INSIGHTS("Insights");

    private final String label;

    ModuleCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
