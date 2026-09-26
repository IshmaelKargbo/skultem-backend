package com.moriba.skultem.domain.vo;

// How one assessment (e.g. "Test 1") is scored.
public enum AssessmentStructure {
    // One score out of 100 - how Skultem has always worked.
    SIMPLE,
    // A continuous-assessment part (several recordings over the period) plus a formal test, combined by the
    // configured percentages.
    CA_AND_TEST
}
