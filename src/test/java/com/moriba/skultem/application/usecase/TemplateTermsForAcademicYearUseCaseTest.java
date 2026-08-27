package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class TemplateTermsForAcademicYearUseCaseTest {

    @DynamicPropertySource
    static void h2Props(DynamicPropertyRegistry registry) {
        // H2 reserves the word YEAR; the ReferenceSequenceEntity column of that name only works
        // against the real Postgres dialect unless we tell H2 to treat it as a normal identifier.
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:skultem;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR");
    }

    @Autowired
    private TemplateTermsForAcademicYearUseCase useCase;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private ReferenceGeneratorUsecase rg;

    @Test
    @Transactional
    void copiesAllThreeTermsToTheNextYear() {
        String schoolId = "school-1";

        var currentYear = AcademicYear.create("ACY-CURRENT", schoolId, "2025/2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
        academicYearRepository.save(currentYear);

        var nextYear = AcademicYear.create("ACY-NEXT", schoolId, "2026/2027",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 31));
        academicYearRepository.save(nextYear);

        for (int i = 1; i <= 3; i++) {
            var termId = rg.generate("TERM", "TRM");
            var term = Term.create(termId, schoolId, currentYear, "Term " + i, i, Term.Status.CLOSED,
                    LocalDate.of(2025, 9, 1).plusMonths((i - 1) * 3L),
                    LocalDate.of(2025, 9, 1).plusMonths((i - 1) * 3L + 2));
            termRepository.save(term);
        }

        System.out.println("Source terms before copy: " + termRepository.findByAcademicYearIdAndSchool("ACY-CURRENT", schoolId).size());

        var created = useCase.execute(schoolId, currentYear, nextYear);

        System.out.println("createdTerms returned by useCase: " + created.size());
        var actual = termRepository.findByAcademicYearIdAndSchool("ACY-NEXT", schoolId);
        System.out.println("Terms actually found in DB for next year: " + actual.size());
        actual.forEach(t -> System.out.println("  -> id=" + t.getId() + " number=" + t.getTermNumber() + " name=" + t.getName()));

        assertThat(created).hasSize(3);
        assertThat(actual).hasSize(3);
    }

    @Test
    @Transactional
    void backfillsRemainingTermsWhenTheNextYearAlreadyHasOne() {
        String schoolId = "school-2";

        var currentYear = AcademicYear.create("ACY-CURRENT-2", schoolId, "2025/2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
        academicYearRepository.save(currentYear);

        var nextYear = AcademicYear.create("ACY-NEXT-2", schoolId, "2026/2027",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 31));
        academicYearRepository.save(nextYear);

        for (int i = 1; i <= 3; i++) {
            var termId = rg.generate("TERM", "TRM");
            var term = Term.create(termId, schoolId, currentYear, "Term " + i, i, Term.Status.CLOSED,
                    LocalDate.of(2025, 9, 1).plusMonths((i - 1) * 3L),
                    LocalDate.of(2025, 9, 1).plusMonths((i - 1) * 3L + 2));
            termRepository.save(term);
        }

        // Simulate the next year already having exactly one term - e.g. an admin manually
        // created its Term 1 (or a previous run partially templated) before configure/assign-next
        // was called. This term number must be left alone.
        var preexisting = Term.create(rg.generate("TERM", "TRM"), schoolId, nextYear, "Term 1 (custom)", 1,
                Term.Status.UPCOMING, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30));
        termRepository.save(preexisting);

        var created = useCase.execute(schoolId, currentYear, nextYear);

        // Only the missing term numbers (2 and 3) should have been backfilled.
        assertThat(created).hasSize(2);
        assertThat(created).extracting(Term::getTermNumber).containsExactly(2, 3);

        var actual = termRepository.findByAcademicYearIdAndSchool("ACY-NEXT-2", schoolId);
        assertThat(actual).hasSize(3);
        assertThat(actual).extracting(Term::getTermNumber).containsExactly(1, 2, 3);

        // The hand-created Term 1 must not have been overridden.
        var term1 = actual.stream().filter(t -> t.getTermNumber() == 1).findFirst().orElseThrow();
        assertThat(term1.getId()).isEqualTo(preexisting.getId());
        assertThat(term1.getName()).isEqualTo("Term 1 (custom)");
    }
}
