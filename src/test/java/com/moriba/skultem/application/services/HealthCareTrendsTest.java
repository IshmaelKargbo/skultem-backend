package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.application.services.HealthCareService.TrendScope;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.HealthCareTurnedAwayRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.repository.TermRepository;

// Complaint trends by week (month), by month (term) and by term (year), with holidays in their own column.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthCareTrendsTest {

    @Mock
    private HealthCareVisitRepository visitRepo;
    @Mock
    private HealthCareTurnedAwayRepository turnedAwayRepo;
    @Mock
    private AcademicYearRepository academicYearRepo;
    @Mock
    private TermRepository termRepo;
    @InjectMocks
    private HealthCareService service;

    private static final Complaint HEADACHE = new Complaint("HEADACHE", "Headache", false);
    private static final Complaint FEVER = new Complaint("FEVER", "Fever / feeling hot", false);

    private static HealthCareVisit visit(String student, String cls, String at, Complaint... complaints) {
        Instant arrived = Instant.parse(at);
        return HealthCareVisit.start("school", student, student, cls, arrived, arrived.plusSeconds(8 * 3600),
                List.of(complaints), null, null, null, null, null, null, "n", "Nurse", arrived);
    }

    private static Term term(String id, String name, String start, String end) {
        var t = mock(Term.class);
        when(t.getId()).thenReturn(id);
        when(t.getName()).thenReturn(name);
        when(t.getStartDate()).thenReturn(LocalDate.parse(start));
        when(t.getEndDate()).thenReturn(LocalDate.parse(end));
        return t;
    }

    private void visits(HealthCareVisit... visits) {
        when(visitRepo.findBetween(eq("school"), any(), any())).thenReturn(List.of(visits));
        when(turnedAwayRepo.findBetween(eq("school"), any(), any())).thenReturn(List.of());
    }

    @Test
    void aMonthIsSplitIntoWeeksAndCountsEachComplaint() {
        var withMedicine = visit("s1", "JSS 1", "2026-10-02T09:00:00Z", HEADACHE, FEVER);
        withMedicine.addMedication("Paracetamol", "500 mg", null, null, "n", "Nurse", Instant.parse("2026-10-02T09:05:00Z"));
        var again = visit("s2", "JSS 2", "2026-10-20T10:00:00Z", HEADACHE);
        again.addMedication("paracetamol ", "500 mg", null, null, "n", "Nurse", Instant.parse("2026-10-20T10:05:00Z"));
        visits(withMedicine, again);

        var t = service.trends("school", TrendScope.MONTH, "2026-10", null, null);

        assertThat(t.buckets()).hasSize(5); // 1-7, 8-14, 15-21, 22-28, 29-31
        var headache = t.complaints().get(0);
        assertThat(headache.label()).isEqualTo("Headache");
        assertThat(headache.counts()).containsExactly(1L, 0L, 1L, 0L, 0L);
        assertThat(headache.total()).isEqualTo(2);
        assertThat(t.complaints()).hasSize(2);
        assertThat(t.medicines()).hasSize(1);
        assertThat(t.medicines().get(0).times()).isEqualTo(2);
        assertThat(t.medicines().get(0).students()).isEqualTo(2);
        assertThat(t.overview().get(0).total()).isEqualTo(2); // visits
    }

    @Test
    void aTermIsSplitIntoItsMonths() {
        var first = term("t1", "First Term", "2026-09-07", "2026-12-18");
        when(termRepo.findByIdAndSchoolId("t1", "school")).thenReturn(Optional.of(first));
        visits(visit("s1", "JSS 1", "2026-09-10T09:00:00Z", FEVER), visit("s1", "JSS 1", "2026-12-01T09:00:00Z", FEVER));

        var t = service.trends("school", TrendScope.TERM, null, "t1", null);

        assertThat(t.buckets()).extracting(b -> b.label()).containsExactly("Sep 2026", "Oct 2026", "Nov 2026", "Dec 2026");
        assertThat(t.buckets().get(0).from()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(t.complaints().get(0).counts()).containsExactly(1L, 0L, 0L, 1L);
        assertThat(t.overview().get(1).total()).isEqualTo(1); // one student
    }

    @Test
    void aYearIsSplitIntoTermsWithHolidaysInTheirOwnColumn() {
        var year = mock(AcademicYear.class);
        when(year.getId()).thenReturn("y1");
        when(year.getName()).thenReturn("2026/2027");
        when(year.getStartDate()).thenReturn(LocalDate.of(2026, 9, 1));
        when(year.getEndDate()).thenReturn(LocalDate.of(2027, 7, 31));
        when(academicYearRepo.findByIdAndSchoolId("y1", "school")).thenReturn(Optional.of(year));
        var first = term("t1", "First Term", "2026-09-07", "2026-12-18");
        var second = term("t2", "Second Term", "2027-01-11", "2027-04-02");
        when(termRepo.findByAcademicYearIdAndSchool("y1", "school")).thenReturn(List.of(second, first));
        visits(visit("s1", "JSS 1", "2026-10-01T09:00:00Z", HEADACHE),
                visit("s2", "JSS 1", "2026-12-28T09:00:00Z", HEADACHE), // Christmas holiday
                visit("s3", "JSS 2", "2027-02-01T09:00:00Z", FEVER));

        var t = service.trends("school", TrendScope.YEAR, null, null, "y1");

        assertThat(t.buckets()).extracting(b -> b.label()).containsExactly("First Term", "Second Term", "Outside terms");
        assertThat(t.complaints().get(0).label()).isEqualTo("Headache");
        assertThat(t.complaints().get(0).counts()).containsExactly(1L, 0L, 1L);
        assertThat(t.classes().get(0).label()).isEqualTo("JSS 1");
        assertThat(t.classes().get(0).total()).isEqualTo(2);
    }
}
