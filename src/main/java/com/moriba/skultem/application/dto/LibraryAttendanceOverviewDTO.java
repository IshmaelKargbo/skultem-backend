package com.moriba.skultem.application.dto;

// The Library Attendance landing figures. libraryPeriods: Library periods on this year's timetable (0 = none set
// up yet, so there are no class sessions).
public record LibraryAttendanceOverviewDTO(int libraryPeriods, long openVisits,
        long unresolvedVisits, long visitsToday, int sessionsToday, int incompleteSessions) {
}
