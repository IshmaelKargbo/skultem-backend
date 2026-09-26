package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.CalendarEvent;
import com.moriba.skultem.domain.model.Notice;
import com.moriba.skultem.domain.repository.CalendarEventRepository;
import com.moriba.skultem.domain.vo.Audience;

@ExtendWith(MockitoExtension.class)
class NoticeCalendarSyncTest {

    private static final String SCHOOL = "school-1";
    private final Instant pta = Instant.parse("2026-10-10T09:00:00Z");

    @Mock
    private CalendarEventRepository calendarRepo;
    @InjectMocks
    private NoticeCalendarSync sync;

    private Notice ptaNotice(Instant at, Instant endsAt) {
        return Notice.create("n1", SCHOOL, "PTA Meeting", "Termly PTA meeting", Notice.Category.EVENT, Audience.PARENTS,
                null, at, endsAt, "School hall", null, "u1", "Head Teacher");
    }

    @Test
    void anEventNoticeMustSayWhenItHappens() {
        assertThatThrownBy(() -> NoticeCalendarSync.validate(Notice.Category.EVENT, null, null, false))
                .isInstanceOf(RuleException.class).hasMessageContaining("when the event takes place");
        assertThatThrownBy(() -> NoticeCalendarSync.validate(Notice.Category.GENERAL, null, null, true))
                .isInstanceOf(RuleException.class).hasMessageContaining("date and time");
        assertThatThrownBy(() -> NoticeCalendarSync.validate(Notice.Category.EVENT, pta, pta.minusSeconds(60), false))
                .isInstanceOf(RuleException.class).hasMessageContaining("can't end before");
        // A plain notice needs none of it.
        NoticeCalendarSync.validate(Notice.Category.GENERAL, null, null, false);
    }

    @Test
    void addingToTheCalendarCreatesAnEventWithADefaultOneHourLength() {
        var notice = ptaNotice(pta, null);

        sync.sync(notice, true);

        var saved = ArgumentCaptor.forClass(CalendarEvent.class);
        verify(calendarRepo).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("PTA Meeting");
        assertThat(saved.getValue().getType()).isEqualTo(CalendarEvent.Type.EVENT);
        assertThat(saved.getValue().getStartDate()).isEqualTo(pta);
        assertThat(saved.getValue().getEndDate()).isEqualTo(pta.plus(Duration.ofHours(1)));
        assertThat(saved.getValue().getLocation()).isEqualTo("School hall");
        assertThat(notice.getCalendarEventId()).isEqualTo(saved.getValue().getId());
    }

    @Test
    void editingTheNoticeUpdatesItsCalendarEntryInsteadOfMakingAnother() {
        var notice = ptaNotice(pta, null);
        var entry = CalendarEvent.create("e1", SCHOOL, "PTA Meeting", "x", CalendarEvent.Type.EVENT, pta,
                pta.plusSeconds(3600), "School hall", "u1", "Head Teacher", null);
        notice.linkCalendarEvent("e1");
        when(calendarRepo.findByIdAndSchool("e1", SCHOOL)).thenReturn(Optional.of(entry));

        var later = pta.plus(Duration.ofDays(7));
        notice.update("PTA Meeting (moved)", "New date", Notice.Category.EVENT, Audience.PARENTS, null, later,
                later.plus(Duration.ofHours(2)), "Library");
        sync.sync(notice, true);

        verify(calendarRepo).save(entry);
        assertThat(entry.getTitle()).isEqualTo("PTA Meeting (moved)");
        assertThat(entry.getStartDate()).isEqualTo(later);
        assertThat(entry.getEndDate()).isEqualTo(later.plus(Duration.ofHours(2)));
        assertThat(entry.getLocation()).isEqualTo("Library");
        assertThat(notice.getCalendarEventId()).isEqualTo("e1");
    }

    @Test
    void takingItOffTheCalendarOrDeletingTheNoticeRemovesTheEntry() {
        var notice = ptaNotice(pta, null);
        var entry = CalendarEvent.create("e1", SCHOOL, "PTA Meeting", "x", CalendarEvent.Type.EVENT, pta,
                pta.plusSeconds(3600), null, "u1", "Head Teacher", null);
        notice.linkCalendarEvent("e1");
        when(calendarRepo.findByIdAndSchool("e1", SCHOOL)).thenReturn(Optional.of(entry));

        sync.sync(notice, false);

        verify(calendarRepo).delete(entry);
        assertThat(notice.getCalendarEventId()).isNull();
    }

    @Test
    void aNoticeWithoutAnEventNeverTouchesTheCalendar() {
        var notice = Notice.create("n2", SCHOOL, "Fees", "Pay up", Notice.Category.FEE, Audience.PARENTS, null, null,
                null, null, null, "u1", "Head Teacher");

        sync.sync(notice, false);

        verify(calendarRepo, never()).save(any());
        verify(calendarRepo, never()).delete(any());
    }
}
