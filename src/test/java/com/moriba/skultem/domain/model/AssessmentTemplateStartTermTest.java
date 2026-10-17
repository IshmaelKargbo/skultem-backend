package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssessmentTemplateStartTermTest {

    @Test
    void aTemplateGradesFromTheFirstTermUnlessToldOtherwise() {
        assertThat(AssessmentTemplate.create("t", "s", "SSS 1", "d", 50).getStartsInTerm()).isEqualTo(1);
    }

    @Test
    void aTemplateCanStartInALaterTermButNeverBeforeTheFirst() {
        var template = AssessmentTemplate.create("t", "s", "SSS 1", "d", 50);

        template.changeStartsInTerm(2);
        assertThat(template.getStartsInTerm()).isEqualTo(2);

        template.changeStartsInTerm(0);
        assertThat(template.getStartsInTerm()).isEqualTo(1);
    }
}
