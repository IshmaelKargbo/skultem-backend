package com.moriba.skultem.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttendanceRateCalculatorTest {

    @Test
    void zeroRecordedDaysProducesAZeroRateRatherThanDividingByZero() {
        assertThat(AttendanceRateCalculator.rate(0, 0)).isEqualTo(0.0);
    }

    @Test
    void rateMatchesTheOriginalComputeClassAttentionUseCaseFormula() {
        // 27/30 = 90.0%, 22/30 = 73.3%, 1/3 = 33.3% - representative fractions the old inline
        // Math.round((present / total) * 1000.0) / 10.0 in ComputeClassAttentionUseCase produced.
        assertThat(AttendanceRateCalculator.rate(27, 30)).isEqualTo(90.0);
        assertThat(AttendanceRateCalculator.rate(22, 30)).isEqualTo(73.3);
        assertThat(AttendanceRateCalculator.rate(1, 3)).isEqualTo(33.3);
    }

    @Test
    void thresholdBoundaryIsExclusiveAtTheConfiguredValue() {
        assertThat(AttendanceRateCalculator.isBelowThreshold(74.9, 75.0)).isTrue();
        assertThat(AttendanceRateCalculator.isBelowThreshold(75.0, 75.0)).isFalse();
        assertThat(AttendanceRateCalculator.isBelowThreshold(75.1, 75.0)).isFalse();
    }

    @Test
    void thresholdIsWhateverTheCallerPassesInNotAFixedGlobalValue() {
        // A school that configures an 80% bar should flag a 78% rate that a 75%-bar school would not.
        assertThat(AttendanceRateCalculator.isBelowThreshold(78.0, 75.0)).isFalse();
        assertThat(AttendanceRateCalculator.isBelowThreshold(78.0, 80.0)).isTrue();
    }

    @Test
    void nullRateIsNeverFlagged() {
        assertThat(AttendanceRateCalculator.isBelowThreshold(null, 75.0)).isFalse();
    }
}
