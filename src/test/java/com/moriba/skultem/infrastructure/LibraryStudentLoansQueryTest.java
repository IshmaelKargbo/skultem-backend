package com.moriba.skultem.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.moriba.skultem.domain.repository.LibraryLoanRepository;

// The student-loans query is only checked against the entities when the app starts - this runs it for real.
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class LibraryStudentLoansQueryTest {

    @Autowired
    LibraryLoanRepository loans;

    @Test
    void studentLoansQueryRuns() {
        assertThat(loans.findByStudent("no-student", "no-school", null, PageRequest.of(0, 5)).getContent()).isEmpty();
        assertThat(loans.findByStudent("no-student", "no-school", List.of("sec"), PageRequest.of(0, 5)).getContent()).isEmpty();
    }
}
