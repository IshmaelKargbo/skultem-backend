package com.moriba.skultem.domain.service;

// The arithmetic of a continuous-assessment ("CA + formal test") result. The two components are each marked
// 0-100 and contribute their configured share of the assessment:
//
//   CA 30% + Formal test 70%, a student with CA 80 (=24/30) and test 80 (=56/70)  ->  24 + 56 = 80/100
//
// The percentages are never assumed - they are the assessment's frozen configuration - and the CA component
// itself is the average of the CA recordings made so far (the individual recordings are kept untouched).
public final class ContinuousAssessmentCalculator {

    private ContinuousAssessmentCalculator() {
    }

    // The CA component (0-100): the average of the recordings so far; null while none has been recorded.
    public static Integer caComponent(java.util.Collection<Integer> recordings) {
        if (recordings == null || recordings.isEmpty()) {
            return null;
        }
        return (int) Math.round(recordings.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    // Points a component contributes to the assessment, e.g. 80 at 30% -> 24.0 (of 30).
    public static double points(Integer component, int percentage) {
        return component == null ? 0 : Math.round(component * percentage / 10.0) / 10.0;
    }

    // The combined assessment score out of 100. A component not recorded yet contributes nothing.
    public static int combine(Integer caComponent, Integer formalComponent, int caPercentage, int formalPercentage) {
        return (int) Math.round((caComponent == null ? 0 : caComponent) * caPercentage / 100.0
                + (formalComponent == null ? 0 : formalComponent) * formalPercentage / 100.0);
    }

    public enum Trend {
        IMPROVING, DECLINING, STEADY, NOT_ENOUGH_DATA
    }

    // Whether the recordings so far are rising or falling: the average of the earlier half against the later half
    // (at least two recordings, and a difference of 5 points or more before it counts as a real trend rather than
    // noise). Gaps (null) are ignored. The same rule the screens use, so a teacher, the approver, the parent and the
    // report always agree.
    public static Trend trend(java.util.List<Integer> recordings) {
        if (recordings == null) {
            return Trend.NOT_ENOUGH_DATA;
        }
        var given = recordings.stream().filter(java.util.Objects::nonNull).toList();
        if (given.size() < 2) {
            return Trend.NOT_ENOUGH_DATA;
        }
        int mid = (given.size() + 1) / 2;
        var earlier = given.subList(0, mid);
        var later = given.size() > mid ? given.subList(mid, given.size()) : given.subList(given.size() - 1, given.size());
        double delta = later.stream().mapToInt(Integer::intValue).average().orElse(0)
                - earlier.stream().mapToInt(Integer::intValue).average().orElse(0);
        return delta >= 5 ? Trend.IMPROVING : delta <= -5 ? Trend.DECLINING : Trend.STEADY;
    }
}
