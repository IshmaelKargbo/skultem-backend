package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.ClassStream;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassStreamRepository;
import com.moriba.skultem.domain.vo.Level;

// SSS 1 runs Art in sections A and B but Science only in A. Starting a new academic year must repeat
// that arrangement - not create every section under every stream (which would invent "Science B").
@ExtendWith(MockitoExtension.class)
class CreateClassSessionsForAcademicYearUseCaseTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private ClassRepository classRepo;
    @Mock
    private ClassSectionRepository classSectionRepo;
    @Mock
    private ClassStreamRepository classStreamRepo;
    @Mock
    private ClassSessionRepository sessionRepo;
    @Mock
    private AcademicYearRepository academicYearRepo;
    @Mock
    private ReferenceGeneratorUsecase rg;
    @Mock
    private CarryForwardClassSessionSetupUseCase carryForward;
    @InjectMocks
    private CreateClassSessionsForAcademicYearUseCase useCase;

    private final Section a = Section.create("sec-a", SCHOOL, "A", "");
    private final Section b = Section.create("sec-b", SCHOOL, "B", "");
    private final Stream art = Stream.create("st-art", "Art", SCHOOL, "");
    private final Stream science = Stream.create("st-sci", "Science", SCHOOL, "");
    private final Clazz sss1 = Clazz.create("cls-1", SCHOOL, null, "SSS 1", Level.SSS, 1);
    private final AcademicYear lastYear = AcademicYear.create("y1", SCHOOL, "2025/2026", LocalDate.of(2025, 9, 1),
            LocalDate.of(2026, 7, 31));
    private final AcademicYear nextYear = AcademicYear.create("y2", SCHOOL, "2026/2027", LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31));

    private List<ClassSession> runWith(List<ClassSession> existing) {
        when(academicYearRepo.findByIdAndSchoolId("y2", SCHOOL)).thenReturn(Optional.of(nextYear));
        Page<Clazz> page = new PageImpl<>(List.of(sss1));
        when(classRepo.findBySchool(anyString(), any(Pageable.class))).thenReturn(page);
        when(classSectionRepo.findByClassIdAndSchoolId("cls-1", SCHOOL)).thenReturn(List.of(
                ClassSection.create("cs-a", SCHOOL, sss1, a), ClassSection.create("cs-b", SCHOOL, sss1, b)));
        when(classStreamRepo.findAllByClassIdAndSchoolId("cls-1", SCHOOL)).thenReturn(List.of(
                ClassStream.create("cst-art", SCHOOL, art, sss1), ClassStream.create("cst-sci", SCHOOL, science, sss1)));
        when(sessionRepo.findAllByClassIdAndSchoolId("cls-1", SCHOOL)).thenReturn(existing);
        lenient().when(sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(anyString(),
                anyString(), anyString(), anyString(), anyString())).thenReturn(false);
        lenient().when(rg.generate(anyString(), anyString())).thenAnswer(i -> UUID.randomUUID().toString());

        useCase.execute(SCHOOL, "y2");

        var saved = ArgumentCaptor.forClass(ClassSession.class);
        var out = new ArrayList<ClassSession>();
        try {
            verify(sessionRepo, atLeastOnce()).save(saved.capture());
            out.addAll(saved.getAllValues());
        } catch (AssertionError e) {
            // nothing saved
        }
        return out;
    }

    private static String pair(ClassSession s) {
        return s.getSection().getName() + "/" + s.getStream().getName();
    }

    @Test
    void repeatsTheSectionsEachStreamActuallyRan() {
        var existing = List.of(
                ClassSession.create("s1", SCHOOL, sss1, art, a, lastYear),
                ClassSession.create("s2", SCHOOL, sss1, art, b, lastYear),
                ClassSession.create("s3", SCHOOL, sss1, science, a, lastYear));

        var created = runWith(existing);

        assertThat(created).extracting(CreateClassSessionsForAcademicYearUseCaseTest::pair)
                .containsExactlyInAnyOrder("A/Art", "B/Art", "A/Science");
    }

    @Test
    void aClassThatNeverHadSessionsGetsEveryCombination() {
        var created = runWith(List.of());

        assertThat(created).extracting(CreateClassSessionsForAcademicYearUseCaseTest::pair)
                .containsExactlyInAnyOrder("A/Art", "B/Art", "A/Science", "B/Science");
    }
}
