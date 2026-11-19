package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// The grade book's two reads, run against the schema the entities generate: a renamed table or column fails here,
// not in production.
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class GetClassGradebookSqlTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void classQueryRunsAgainstTheSchema() {
        assertThat(jdbc.queryForList(GetClassGradebookUseCase.CLASS_SQL, "no-session", "no-school")).isEmpty();
    }

    @Test
    void marksQueryRunsAgainstTheSchema() {
        assertThat(jdbc.queryForList(GetClassGradebookUseCase.LINES_SQL, "no-school", "no-session", "no-term"))
                .isEmpty();
    }
}
