package com.moriba.skultem.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.moriba.skultem.application.usecase.ListStudentBehavioursUseCase;

// The per-student behaviour queries are only checked against the entities when the app starts - this runs them.
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class StudentBehaviourQueryTest {

    @Autowired
    ListStudentBehavioursUseCase useCase;

    @Test
    void studentBehaviourQueriesRun() {
        assertThat(useCase.execute("no-school", "no-student", 0, 5).getContent()).isEmpty();
        var summary = useCase.summary("no-school", "no-student");
        assertThat(summary.total()).isZero();
        assertThat(summary.negative()).isZero();
    }
}
