package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

class AssessmentConfigurationTest {

    private AssessmentConfiguration ca(int ca, int formal, int entries) {
        return AssessmentConfiguration.create("school", "section", AssessmentStructure.CA_AND_TEST, ca, formal,
                CaFrequency.WEEKLY, entries, "admin-1");
    }

    @Test
    void aContinuousConfigurationKeepsItsPercentagesFrequencyAndRecordings() {
        var c = ca(30, 70, 6);

        assertThat(c.isContinuous()).isTrue();
        assertThat(c.getCaPercentage()).isEqualTo(30);
        assertThat(c.getFormalPercentage()).isEqualTo(70);
        assertThat(c.getCaFrequency()).isEqualTo(CaFrequency.WEEKLY);
        assertThat(c.getCaEntries()).isEqualTo(6);
        assertThat(c.getVersion()).isEqualTo(1);
    }

    @Test
    void percentagesMustAddUpToOneHundredAndBothBePositive() {
        assertThatThrownBy(() -> ca(30, 60, 6)).isInstanceOf(RuleException.class).hasMessageContaining("100%");
        assertThatThrownBy(() -> ca(0, 100, 6)).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> ca(100, 0, 6)).isInstanceOf(RuleException.class);
    }

    @Test
    void continuousAssessmentNeedsAFrequencyAndAReasonableNumberOfRecordings() {
        assertThatThrownBy(() -> AssessmentConfiguration.create("s", null, AssessmentStructure.CA_AND_TEST, 30, 70,
                null, 6, "u")).isInstanceOf(RuleException.class).hasMessageContaining("how often");
        assertThatThrownBy(() -> ca(30, 70, 0)).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> ca(30, 70, AssessmentConfiguration.MAX_CA_ENTRIES + 1))
                .isInstanceOf(RuleException.class);
    }

    @Test
    void aSimpleConfigurationIgnoresAnyCaSettingsAndIsASingleScore() {
        var c = AssessmentConfiguration.create("s", null, AssessmentStructure.SIMPLE, 30, 70, CaFrequency.DAILY, 9, "u");

        assertThat(c.isContinuous()).isFalse();
        assertThat(c.getCaPercentage()).isZero();
        assertThat(c.getFormalPercentage()).isEqualTo(100);
        assertThat(c.getCaFrequency()).isNull();
        assertThat(c.getCaEntries()).isZero();
    }

    @Test
    void everyChangeBumpsTheVersionAndRecordsWhoMadeIt() {
        var c = ca(30, 70, 6);

        c.change(AssessmentStructure.CA_AND_TEST, 40, 60, CaFrequency.DAILY, 10, "owner-1");

        assertThat(c.getVersion()).isEqualTo(2);
        assertThat(c.getUpdatedByUserId()).isEqualTo("owner-1");
        assertThat(c.summary()).contains("CA 40%").contains("Formal 60%").contains("DAILY").contains("x10");
    }

    @Test
    void anInvalidChangeLeavesTheConfigurationUntouched() {
        var c = ca(30, 70, 6);

        assertThatThrownBy(() -> c.change(AssessmentStructure.CA_AND_TEST, 50, 40, CaFrequency.WEEKLY, 6, "x"))
                .isInstanceOf(RuleException.class);

        assertThat(c.getVersion()).isEqualTo(1);
        assertThat(c.getCaPercentage()).isEqualTo(30);
    }

    @Test
    void theDefaultIsTheSingleScoreAssessmentsHaveAlwaysBeen() {
        var d = AssessmentConfiguration.simple("s", null);

        assertThat(d.isDefault()).isTrue();
        assertThat(d.isContinuous()).isFalse();
    }
}
