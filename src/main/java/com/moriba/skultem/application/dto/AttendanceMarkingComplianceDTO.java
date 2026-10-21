package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

// How completely each class's attendance has been marked this term so far: days with a register over
// every school day the class owed one - from the later of the term start and its first student
// (countedFrom) - with the days still missing and who's responsible (its active class masters).
// schoolDays at the top is the whole term so far; percentage is null for a class with no students.
public record AttendanceMarkingComplianceDTO(
        String termId,
        String termName,
        LocalDate startDate,
        LocalDate asOf,
        int schoolDays,
        List<ClassMarking> classes) {

    public record ClassMarking(
            String classSessionId,
            String className,
            String sectionName,
            String streamName,
            List<String> classMasters,
            LocalDate countedFrom,
            int schoolDays,
            int markedDays,
            Double percentage,
            LocalDate lastMarked,
            List<LocalDate> missingDays) {
    }
}
