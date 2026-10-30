package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One row of the library session list. {@code id} is null for an UPCOMING session nobody has opened yet -
 * it is identified by its timetable entry and date until then.
 * status: UPCOMING, IN_PROGRESS, COMPLETED or CANCELLED. incomplete = the period has ended and the
 * register isn't finalized (nobody is marked absent for that).
 */
public record LibrarySessionDTO(String id, String timetableEntryId, LocalDate date, String classSessionId,
        String className, String subjectName, String periodName, LocalTime startTime, LocalTime endTime,
        String roomName, String status, boolean incomplete, boolean editable, String cancelReason, int rosterCount,
        int present, int absent, int excused, int unmarked) {
}
