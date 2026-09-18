package com.moriba.skultem.domain.service;

// Single place a pass mark is turned into a pass rate, mirroring what AttendanceRateCalculator
// does for attendance thresholds - used by the Academic Report's overview/subject/student
// performance sections so "did this student/subject pass" can't be computed two different ways.
public final class AcademicPerformanceCalculator {

    private AcademicPerformanceCalculator() {
    }

    public static boolean isPassed(double average, int passMark) {
        return average >= passMark;
    }

    public static double passRate(long passCount, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((passCount / (double) total) * 1000.0) / 10.0;
    }

    public static double round1Dp(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
