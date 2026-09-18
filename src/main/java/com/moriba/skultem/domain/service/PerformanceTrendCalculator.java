package com.moriba.skultem.domain.service;

import java.util.List;

// Turns a chronological series of approved assessment averages into a trend label, for the
// Performance Trends report section and the "declining trend" signal in Students Requiring
// Attention. Deliberately just compares the first and last data point - no smoothing or arbitrary
// stability band - so the label is always directly traceable to the two real values it's based on,
// per the "do not make unsupported claims" requirement.
public final class PerformanceTrendCalculator {

    public enum Trend {
        IMPROVING,
        DECLINING,
        STABLE,
        INSUFFICIENT_DATA
    }

    private PerformanceTrendCalculator() {
    }

    public static Trend evaluate(List<Double> chronologicalAverages) {
        if (chronologicalAverages == null || chronologicalAverages.size() < 2) {
            return Trend.INSUFFICIENT_DATA;
        }

        double first = chronologicalAverages.get(0);
        double last = chronologicalAverages.get(chronologicalAverages.size() - 1);

        if (last > first) {
            return Trend.IMPROVING;
        }
        if (last < first) {
            return Trend.DECLINING;
        }
        return Trend.STABLE;
    }
}
