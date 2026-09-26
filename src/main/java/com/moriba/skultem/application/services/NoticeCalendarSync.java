package com.moriba.skultem.application.services;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.CalendarEvent;
import com.moriba.skultem.domain.model.Notice;
import com.moriba.skultem.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;

// A notice about something that happens on a date (a PTA meeting, a sports day) can also sit on the
// school calendar. The calendar entry is created from the notice and kept in step with it - edit the
// notice and the entry follows; take it off the calendar, or delete the notice, and the entry goes -
// so the two never drift apart. The notice remembers its entry in calendarEventId.
@Service
@RequiredArgsConstructor
public class NoticeCalendarSync {

    // An event with no stated end still needs one on the calendar.
    private static final Duration DEFAULT_LENGTH = Duration.ofHours(1);

    private final CalendarEventRepository calendarRepo;

    // The rules for the event part of a notice; throws RuleException with the reason.
    public static void validate(Notice.Category category, Instant eventAt, Instant eventEndsAt,
            boolean addToCalendar) {
        if (category == Notice.Category.EVENT && eventAt == null) {
            throw new RuleException("Say when the event takes place");
        }
        if (addToCalendar && eventAt == null) {
            throw new RuleException("Pick the date and time of the event to put it on the calendar");
        }
        if (eventEndsAt != null && eventAt == null) {
            throw new RuleException("An end time needs a start - pick when the event begins");
        }
        if (eventEndsAt != null && eventEndsAt.isBefore(eventAt)) {
            throw new RuleException("The event can't end before it starts");
        }
    }

    // Brings the notice's calendar entry in line with what was asked: created, updated, or removed.
    public void sync(Notice notice, boolean addToCalendar) {
        var existing = notice.getCalendarEventId() == null ? null
                : calendarRepo.findByIdAndSchool(notice.getCalendarEventId(), notice.getSchoolId()).orElse(null);

        if (!addToCalendar || notice.getEventAt() == null) {
            if (existing != null) {
                calendarRepo.delete(existing);
            }
            if (notice.getCalendarEventId() != null) {
                notice.linkCalendarEvent(null);
            }
            return;
        }

        Instant end = notice.getEventEndsAt() != null ? notice.getEventEndsAt()
                : notice.getEventAt().plus(DEFAULT_LENGTH);

        if (existing != null) {
            existing.update(notice.getTitle(), notice.getContent(), CalendarEvent.Type.EVENT, notice.getEventAt(),
                    end, notice.getEventLocation());
            calendarRepo.save(existing);
            return;
        }

        var entry = CalendarEvent.create(UUID.randomUUID().toString(), notice.getSchoolId(), notice.getTitle(),
                notice.getContent(), CalendarEvent.Type.EVENT, notice.getEventAt(), end, notice.getEventLocation(),
                notice.getPostedByUserId(), notice.getPostedByName(), notice.getManagementSectionId());
        calendarRepo.save(entry);
        notice.linkCalendarEvent(entry.getId());
    }

    // The notice is going away - so does its calendar entry.
    public void remove(Notice notice) {
        sync(notice, false);
    }
}
