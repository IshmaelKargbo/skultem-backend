package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;

// D-9: a required Library subject puts the whole class on the roster; an optional one only those who take it.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibraryRosterResolverTest {

    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private ClassSubjectRepository classSubjectRepo;
    @Mock
    private StreamSubjectRepository streamSubjectRepo;
    @Mock
    private EnrollmentSubjectRepository enrollmentSubjectRepo;
    @InjectMocks
    private LibraryRosterResolver resolver;

    private ClassSession session;
    private Section section;

    @BeforeEach
    void setUp() {
        var clazz = mock(Clazz.class);
        when(clazz.getId()).thenReturn("class-1");
        var year = mock(AcademicYear.class);
        when(year.getId()).thenReturn("year");
        section = mock(Section.class);
        when(section.getId()).thenReturn("sec-a");
        session = mock(ClassSession.class);
        when(session.getClazz()).thenReturn(clazz);
        when(session.getAcademicYear()).thenReturn(year);
        when(session.getSection()).thenReturn(section);

        // e3 has left; e4 is in another section of the same class.
        var enrollments = List.of(enrollment("e1", Enrollment.Status.ACTIVE, section),
                enrollment("e2", Enrollment.Status.ACTIVE, section),
                enrollment("e3", Enrollment.Status.LEFT, section),
                enrollment("e4", Enrollment.Status.ACTIVE, otherSection()));
        when(enrollmentRepo.findAllByClassAndAcademicAndSchoolId(eq("class-1"), eq("year"), eq("school"), any()))
                .thenReturn(new PageImpl<>(enrollments));
    }

    private static Section otherSection() {
        var other = mock(Section.class);
        when(other.getId()).thenReturn("sec-b");
        return other;
    }

    private Enrollment enrollment(String id, Enrollment.Status status, Section in) {
        var e = mock(Enrollment.class);
        when(e.getId()).thenReturn(id);
        when(e.getStatus()).thenReturn(status);
        when(e.getSection()).thenReturn(in);
        return e;
    }

    private void libraryIs(boolean mandatory) {
        var assignment = mock(ClassSubject.class);
        when(assignment.getMandatory()).thenReturn(mandatory);
        when(classSubjectRepo.findByClassIdAndSubjectIdAndSchoolId("class-1", "lib", "school"))
                .thenReturn(Optional.of(assignment));
    }

    @Test
    void aRequiredLibrarySubjectPutsTheWholeSectionOnTheRoster() {
        libraryIs(true);

        assertThat(resolver.roster("school", session, "lib")).extracting(Enrollment::getId)
                .containsExactly("e1", "e2");
    }

    @Test
    void anOptionalLibrarySubjectPutsOnlyItsStudentsOnTheRoster() {
        libraryIs(false);
        when(enrollmentSubjectRepo.existsByEnrollmentIdAndSubjectIdAndSchoolId("e2", "lib", "school"))
                .thenReturn(true);

        assertThat(resolver.roster("school", session, "lib")).extracting(Enrollment::getId).containsExactly("e2");
    }

    @Test
    void withNoSubjectAssignmentTheWholeSectionIsOnTheRoster() {
        assertThat(resolver.roster("school", session, "lib")).extracting(Enrollment::getId)
                .containsExactly("e1", "e2");
    }
}
