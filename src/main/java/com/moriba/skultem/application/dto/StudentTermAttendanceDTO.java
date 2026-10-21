package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

// Term-to-date attendance per student: present-or-late days over every school day so far, so a day
// nobody marked counts against the figure instead of disappearing. schoolDays at the top is the whole
// term so far; each student's own schoolDays starts from when they enrolled, if that was later
// (countedFrom), and percentage is null while that's still 0 days.
public record StudentTermAttendanceDTO(
        String termId,
        String termName,
        LocalDate startDate,
        LocalDate asOf,
        int schoolDays,
        List<StudentRow> students) {

    public record StudentRow(String studentId, LocalDate countedFrom, int schoolDays, long presentOrLate,
            Double percentage) {
    }
}
