package com.moriba.skultem.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ContinuousAssessmentCalculatorTest {

    @Test
    void thirtySeventyExampleFromTheSpec_CA24of30PlusTest56of70Is80() {
        // CA 80% of 30 = 24; formal 80% of 70 = 56.
        assertThat(ContinuousAssessmentCalculator.points(80, 30)).isEqualTo(24.0);
        assertThat(ContinuousAssessmentCalculator.points(80, 70)).isEqualTo(56.0);
        assertThat(ContinuousAssessmentCalculator.combine(80, 80, 30, 70)).isEqualTo(80);
    }

    @Test
    void thePercentagesAreConfigurationNotConstants() {
        assertThat(ContinuousAssessmentCalculator.combine(80, 60, 40, 60)).isEqualTo(68);  // 32 + 36
        assertThat(ContinuousAssessmentCalculator.combine(80, 60, 50, 50)).isEqualTo(70);  // 40 + 30
        assertThat(ContinuousAssessmentCalculator.combine(100, 100, 30, 70)).isEqualTo(100);
        assertThat(ContinuousAssessmentCalculator.combine(0, 0, 30, 70)).isZero();
    }

    @Test
    void theCaComponentIsTheAverageOfTheRecordingsSoFar() {
        assertThat(ContinuousAssessmentCalculator.caComponent(List.of(80, 75, 70, 55, 45))).isEqualTo(65);
        assertThat(ContinuousAssessmentCalculator.caComponent(List.of(90))).isEqualTo(90);
        assertThat(ContinuousAssessmentCalculator.caComponent(List.of())).isNull();
    }

    @Test
    void aComponentNotRecordedYetContributesNothing() {
        assertThat(ContinuousAssessmentCalculator.combine(80, null, 30, 70)).isEqualTo(24);
        assertThat(ContinuousAssessmentCalculator.combine(null, 80, 30, 70)).isEqualTo(56);
        assertThat(ContinuousAssessmentCalculator.combine(null, null, 30, 70)).isZero();
    }

    @Test
    void theTrendComparesTheEarlierRecordingsToTheLaterOnes() {
        var t = ContinuousAssessmentCalculator.Trend.class;
        assertThat(ContinuousAssessmentCalculator.trend(java.util.Arrays.asList(80, 75, 70, 55, 45, 40)))
                .isEqualTo(ContinuousAssessmentCalculator.Trend.DECLINING);
        assertThat(ContinuousAssessmentCalculator.trend(java.util.Arrays.asList(40, 45, 55, 70, 75, 80)))
                .isEqualTo(ContinuousAssessmentCalculator.Trend.IMPROVING);
        assertThat(ContinuousAssessmentCalculator.trend(java.util.Arrays.asList(60, 62, 58, 61)))
                .isEqualTo(ContinuousAssessmentCalculator.Trend.STEADY);
    }

    @Test
    void oneRecordingOrGapsAloneAreNotATrend() {
        assertThat(ContinuousAssessmentCalculator.trend(java.util.Arrays.asList(70, null, null)))
                .isEqualTo(ContinuousAssessmentCalculator.Trend.NOT_ENOUGH_DATA);
        assertThat(ContinuousAssessmentCalculator.trend(null))
                .isEqualTo(ContinuousAssessmentCalculator.Trend.NOT_ENOUGH_DATA);
        assertThat(ContinuousAssessmentCalculator.trend(java.util.Arrays.asList(90, null, 60)))
                .isEqualTo(ContinuousAssessmentCalculator.Trend.DECLINING);
    }

    @Test
    void whenCaIsOnlyMonitoredItNeverChangesTheScore() {
        // CA 0% / formal 100%: the score is the formal test, whatever the CA was - strong, weak or missing.
        assertThat(ContinuousAssessmentCalculator.combine(95, 55, 0, 100)).isEqualTo(55);
        assertThat(ContinuousAssessmentCalculator.combine(10, 55, 0, 100)).isEqualTo(55);
        assertThat(ContinuousAssessmentCalculator.combine(null, 55, 0, 100)).isEqualTo(55);
        assertThat(ContinuousAssessmentCalculator.points(95, 0)).isEqualTo(0.0);
    }
}
