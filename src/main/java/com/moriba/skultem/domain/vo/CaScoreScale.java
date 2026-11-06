package com.moriba.skultem.domain.vo;

// What a teacher's CA mark is out of. The stored recording is always 0-100; this only changes data entry.
public enum CaScoreScale {
    // Out of 100 - the default.
    PERCENT,
    // Out of the recording's own weight: 2 on a 3% week (stored as 67).
    WEIGHT
}
