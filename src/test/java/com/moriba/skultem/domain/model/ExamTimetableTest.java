package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;

class ExamTimetableTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 7);

    private ExamPaper paper(String start, String end, List<String> classes) {
        return ExamPaper.create("s", "tt", "maths", "Paper 1", DAY, LocalTime.parse(start), LocalTime.parse(end),
                "Hall", null, classes, List.of());
    }

    @Test
    void aTimetableStartsAsADraftAndPublishes() {
        var t = ExamTimetable.create("s", "term", "exam", "First Term Exam", DAY, DAY.plusDays(9), null);
        assertThat(t.isPublished()).isFalse();
        t.publish("Exam Officer");
        assertThat(t.isPublished()).isTrue();
        assertThat(t.getPublishedByName()).isEqualTo("Exam Officer");
        t.unpublish();
        assertThat(t.isPublished()).isFalse();
    }

    @Test
    void theExamPeriodCantEndBeforeItStarts() {
        assertThatThrownBy(() -> ExamTimetable.create("s", "term", "exam", "Exam", DAY, DAY.minusDays(1), null))
                .isInstanceOf(RuleException.class);
    }

    @Test
    void theExamPeriodBoundsItsDates() {
        var t = ExamTimetable.create("s", "term", "exam", "Exam", DAY, DAY.plusDays(4), null);
        assertThat(t.covers(DAY.plusDays(2))).isTrue();
        assertThat(t.covers(DAY.plusDays(5))).isFalse();
    }

    @Test
    void onePaperCanBeSatBySeveralClasses() {
        var p = paper("09:00", "11:00", List.of("jss1a", "jss1b", "jss1b", " "));
        assertThat(p.getClassIds()).containsExactly("jss1a", "jss1b");
    }

    @Test
    void aPaperNeedsAClassAndToEndAfterItStarts() {
        assertThatThrownBy(() -> paper("09:00", "11:00", List.of())).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> paper("11:00", "09:00", List.of("a"))).isInstanceOf(RuleException.class);
    }

    @Test
    void papersBackToBackDontClashButOverlappingOnesDo() {
        var morning = paper("09:00", "11:00", List.of("a"));
        assertThat(morning.overlaps(paper("11:00", "13:00", List.of("a")))).isFalse();
        assertThat(morning.overlaps(paper("10:30", "12:00", List.of("a")))).isTrue();
    }
}
