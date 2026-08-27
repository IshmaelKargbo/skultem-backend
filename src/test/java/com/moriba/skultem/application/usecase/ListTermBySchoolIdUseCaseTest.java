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
class ListTermBySchoolIdUseCaseTest {

    @DynamicPropertySource
    static void h2Props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:skultem2;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR");
    }

    @Autowired
    private ListTermBySchoolIdUseCase useCase;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private ReferenceGeneratorUsecase rg;

    @Test
    @Transactional
    void listsTermsForTheYearThatIsActuallyActive() {
        String schoolId = "school-list-1";

        var currentYear = AcademicYear.create("ACY-CUR", schoolId, "2025/2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 31));
        academicYearRepository.save(currentYear);

        var nextYear = AcademicYear.create("ACY-NEXT", schoolId, "2026/2027",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 31));
        academicYearRepository.save(nextYear);

        var currentTerm = Term.create(rg.generate("TERM", "TRM"), schoolId, currentYear, "Term 1", 1,
                Term.Status.CLOSED, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 1));
        termRepository.save(currentTerm);

        var nextTerm = Term.create(rg.generate("TERM", "TRM"), schoolId, nextYear, "Term 1", 1,
                Term.Status.UPCOMING, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
        termRepository.save(nextTerm);

        // Nobody is active yet.
        assertThat(academicYearRepository.findActiveBySchool(schoolId)).isEmpty();

        // Switch active year to "next" - mirrors what ActiveAcademicYearUseCase /
        // CloseAcademicYearAndActivateNextUseCase do.
        academicYearRepository.deactivateAllBySchool(schoolId);
        nextYear.setActive(true);
        academicYearRepository.save(nextYear);

        var activeNow = academicYearRepository.findActiveBySchool(schoolId).orElseThrow();
        System.out.println("Active year after switch: " + activeNow.getId() + " (" + activeNow.getName() + ")");
        assertThat(activeNow.getId()).isEqualTo("ACY-NEXT");

        // This is exactly what GET /term?page=1&size=6 does - no academicYearId param.
        var page = useCase.execute(schoolId, 0, 10);
        System.out.println("Terms returned with no academicYearId filter: " + page.getTotalElements());
        page.getContent().forEach(t -> System.out.println("  -> " + t.name() + " / year=" + t.academicYear().id()));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).academicYear().id()).isEqualTo("ACY-NEXT");

        // This is what the frontend's header year-switcher makes the request look like once a
        // viewing year is picked - an explicit academicYearId query param.
        var explicit = useCase.execute(schoolId, "ACY-NEXT", 0, 10);
        System.out.println("Terms returned with explicit academicYearId=ACY-NEXT: " + explicit.getTotalElements());
        assertThat(explicit.getContent()).hasSize(1);

        // And the old year, explicitly, should show its own term - not the new one's.
        var oldExplicit = useCase.execute(schoolId, "ACY-CUR", 0, 10);
        System.out.println("Terms returned with explicit academicYearId=ACY-CUR: " + oldExplicit.getTotalElements());
        assertThat(oldExplicit.getContent()).hasSize(1);
        assertThat(oldExplicit.getContent().get(0).academicYear().id()).isEqualTo("ACY-CUR");
    }
}
