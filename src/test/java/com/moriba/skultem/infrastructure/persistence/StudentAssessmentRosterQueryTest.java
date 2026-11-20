package com.moriba.skultem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.infrastructure.persistence.jpa.StudentAssessmentJpaRepository;

// The grading roster's query (students who left excluded) runs against the schema the entities generate.
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class StudentAssessmentRosterQueryTest {

    @Autowired
    StudentAssessmentJpaRepository repo;

    @Test
    void theRosterQueryRuns() {
        assertThat(repo.findAllBySubjectAndSessionAndTermId("no-subject", "no-session", "no-term",
                Enrollment.Status.LEFT)).isEmpty();
    }
}
