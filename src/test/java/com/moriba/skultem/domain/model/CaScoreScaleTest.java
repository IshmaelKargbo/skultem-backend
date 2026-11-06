package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;
import com.moriba.skultem.domain.vo.CaScoreScale;

// Whether teachers mark a CA recording out of 100 or out of its own weight. Out of 100 unless the school says so.
class CaScoreScaleTest {

    private static AssessmentConfiguration config() {
        return AssessmentConfiguration.create("school", "sec", AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 10, "admin");
    }

    private static ClassSubjectAssessmentLifeCycle cycle() {
        return ClassSubjectAssessmentLifeCycle.create("c1", "school", mock(TeacherSubject.class), mock(Term.class),
                mock(Assessment.class), Status.DRAFT);
    }

    @Test
    void marksAreOutOf100ByDefault() {
        var cycle = cycle();
        cycle.freezeStructure(config());

        assertThat(config().getCaScoreScale()).isEqualTo(CaScoreScale.PERCENT);
        assertThat(cycle.getCaScoreScale()).isEqualTo(CaScoreScale.PERCENT);
    }

    @Test
    void anAssessmentOpensWithTheSchoolsScale() {
        var config = config();
        config.changeScoreScale(CaScoreScale.WEIGHT);
        var cycle = cycle();

        cycle.freezeStructure(config);

        assertThat(cycle.getCaScoreScale()).isEqualTo(CaScoreScale.WEIGHT);
    }

    @Test
    void savingWithoutTheOptionKeepsTheCurrentScale() {
        var config = config();
        config.changeScoreScale(CaScoreScale.WEIGHT);

        config.changeScoreScale(null);

        assertThat(config.getCaScoreScale()).isEqualTo(CaScoreScale.WEIGHT);
    }

    @Test
    void anOpenAssessmentFollowsAChangedScaleOnlyWhenItDiffers() {
        var cycle = cycle();
        cycle.freezeStructure(config());

        assertThat(cycle.applyScoreScale(CaScoreScale.PERCENT)).isFalse();
        assertThat(cycle.applyScoreScale(CaScoreScale.WEIGHT)).isTrue();
        assertThat(cycle.getCaScoreScale()).isEqualTo(CaScoreScale.WEIGHT);
    }
}
