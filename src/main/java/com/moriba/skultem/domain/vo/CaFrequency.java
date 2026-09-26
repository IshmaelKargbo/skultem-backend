package com.moriba.skultem.domain.vo;

// How often continuous assessment is recorded - it shapes the teacher's recording workflow (and names the
// recordings: "Day 3", "Week 2", "Mid-week 4", "Activity 5"), not the formal report structure.
public enum CaFrequency {
    DAILY("Day"),
    WEEKLY("Week"),
    MID_WEEK("Mid-week"),
    // Any other rhythm the school follows: recordings are simply numbered activities.
    CUSTOM("Activity");

    private final String unit;

    CaFrequency(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }
}
