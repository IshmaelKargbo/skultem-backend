package com.moriba.skultem.domain.service;

// Single place the "present-or-late over total recorded (non-holiday) days" percentage is computed,
// and the single place a computed rate is compared against a school's configured attendance
// threshold (School.getAttendanceThreshold() - there is no global constant, each school sets its
// own bar via PUT /api/v1/school). Used by ComputeClassAttentionUseCase and the Monthly/Term
// Summary + Inspection Report use cases so the 75%-style warning can never drift between them.
public final class AttendanceRateCalculator {

    private AttendanceRateCalculator() {
    }

    public static double rate(long presentOrLate, long totalRecorded) {
        if (totalRecorded == 0) {
            return 0.0;
        }
        return Math.round((presentOrLate / (double) totalRecorded) * 1000.0) / 10.0;
    }

    public static boolean isBelowThreshold(Double rate, double threshold) {
        return rate != null && rate < threshold;
    }
}
