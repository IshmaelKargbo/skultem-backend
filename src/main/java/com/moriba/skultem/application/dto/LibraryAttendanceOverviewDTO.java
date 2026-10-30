package com.moriba.skultem.application.dto;

// The Library Attendance landing figures. librarySubjectId is null until the school picks its Library subject.
public record LibraryAttendanceOverviewDTO(String librarySubjectId, String librarySubjectName, long openVisits,
        long unresolvedVisits, long visitsToday, int sessionsToday, int incompleteSessions) {
}
