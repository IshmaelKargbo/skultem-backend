package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaComponent;
import com.moriba.skultem.domain.vo.CaFrequency;

// Attendance mark = (days present + late) over the term's school days, as a percentage.
class AttendanceCaServiceTest {

    private final AttendanceRepository attendanceRepo = mock(AttendanceRepository.class);
    private final AssessmentCaEntryRepository caEntryRepo = mock(AssessmentCaEntryRepository.class);
    private final AttendanceCaService service = new AttendanceCaService(attendanceRepo, caEntryRepo);

    private ClassSubjectAssessmentLifeCycle cycleWithAttendance() {
        var config = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 60, 40,
                CaFrequency.WEEKLY, 6, "admin");
        config.replaceComponents(List.of(new CaComponent("Attendance", 20, CaComponent.ATTENDANCE),
                new CaComponent("Assignment", 40)), "Test");
        var term = mock(com.moriba.skultem.domain.model.Term.class);
        when(term.getStartDate()).thenReturn(LocalDate.of(2026, 9, 1));
        when(term.getEndDate()).thenReturn(LocalDate.of(2026, 12, 15));
        var cycle = ClassSubjectAssessmentLifeCycle.create("c", "school", mock(TeacherSubject.class), term,
                mock(com.moriba.skultem.domain.model.Assessment.class), Status.DRAFT);
        cycle.freezeStructure(config);
        return cycle;
    }

    private AssessmentScore score(String id, String enrollmentId) {
        var score = mock(AssessmentScore.class, RETURNS_DEEP_STUBS);
        when(score.getId()).thenReturn(id);
        when(score.getStudentAssessment().getEnrollment().getId()).thenReturn(enrollmentId);
        return score;
    }

    @Test
    void presentAndLateDaysOverTheSchoolDays() {
        var cycle = cycleWithAttendance();
        var ts = mock(TeacherSubject.class, RETURNS_DEEP_STUBS);
        when(ts.getSession().getStream()).thenReturn(null);
        // enrollmentId, studentId, name, name, admission, gender, presentOrLate, late, recorded
        when(attendanceRepo.attendanceCountsBySessionAndDateRange(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        new Object[] { "e1", "s1", "A", "B", "1", null, 45L, 5L, 50L },
                        new Object[] { "e2", "s2", "C", "D", "2", null, 40L, 0L, 50L }));
        var s1 = score("sc1", "e1");
        var s2 = score("sc2", "e2");

        var result = service.sync("school", ts, cycle, List.of(s1, s2), "teacher");

        assertThat(result.schoolDays()).isEqualTo(50);
        assertThat(result.entries()).extracting(e -> e.getEntryNumber() + ":" + e.getScore())
                .containsExactlyInAnyOrder("1:90", "1:80");
        assertThat(result.scores()).hasSize(2);
    }

    @Test
    void aTermWithNoAttendanceRecordedChangesNothing() {
        var cycle = cycleWithAttendance();
        var ts = mock(TeacherSubject.class, RETURNS_DEEP_STUBS);
        when(attendanceRepo.attendanceCountsBySessionAndDateRange(anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        var result = service.sync("school", ts, cycle, List.of(score("sc1", "e1")), "teacher");

        assertThat(result.entries()).isEmpty();
    }

    @Test
    void anAssessmentWithoutAnAttendancePartIsLeftAlone() {
        var config = AssessmentConfiguration.create("school", null, AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin");
        var cycle = ClassSubjectAssessmentLifeCycle.create("c", "school", mock(TeacherSubject.class),
                mock(com.moriba.skultem.domain.model.Term.class),
                mock(com.moriba.skultem.domain.model.Assessment.class), Status.DRAFT);
        cycle.freezeStructure(config);

        var result = service.sync("school", mock(TeacherSubject.class), cycle, List.of(score("sc1", "e1")), "t");

        assertThat(result.entries()).isEmpty();
        assertThat(cycle.attendanceEntryNumber()).isZero();
    }
}
