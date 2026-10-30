package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

// B-5 / B-12: a Library period takes the whole period for the class, so its roster is the whole class.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibraryRosterResolverTest {

    @Mock
    private EnrollmentRepository enrollmentRepo;
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

    @Test
    void theWholeSectionIsOnTheRosterLeaversAndOtherSectionsAreNot() {
        assertThat(resolver.roster("school", session)).extracting(Enrollment::getId).containsExactly("e1", "e2");
    }
}
