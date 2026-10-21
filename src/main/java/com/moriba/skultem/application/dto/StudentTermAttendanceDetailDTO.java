package com.moriba.skultem.application.dto;

import java.time.LocalDate;

// One student's attendance for a term so far, over every school day since the later of the term start
// and their enrollment (countedFrom): percentage is present-or-late over schoolDays (null while that's
// 0), and notMarked is the school days with no record at all (nobody took the register).
public record StudentTermAttendanceDetailDTO(
        String termId,
        String termName,
        LocalDate startDate,
        LocalDate asOf,
        LocalDate countedFrom,
        int schoolDays,
        long present,
        long late,
        long absent,
        long excused,
        long notMarked,
        Double percentage) {
}
