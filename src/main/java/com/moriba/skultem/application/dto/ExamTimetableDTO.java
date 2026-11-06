package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// One assessment's exam timetable for a term, with its papers. classes are the classes that sit this assessment (that
// the caller can see); missing lists, per class, the subjects with no paper yet - only for the exam office.
public record ExamTimetableDTO(
        String id,
        String termId,
        String termName,
        LocalDate termStartDate,
        LocalDate termEndDate,
        String assessmentId,
        String assessmentName,
        String templateName,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String instructions,
        String status,
        Instant publishedAt,
        String publishedByName,
        List<Ref> classes,
        List<Paper> papers,
        List<Missing> missing) {

    public record Ref(String id, String name) {
    }

    public record Paper(String id, String subjectId, String subjectName, String paper, LocalDate examDate,
            LocalTime startTime, LocalTime endTime, String venue, String notes, List<Ref> classes,
            List<Ref> invigilators) {
    }

    public record Missing(String classId, String className, List<Ref> subjects) {
    }

    // One assessment in the term, with its timetable if it has one - the list the timetable page starts from.
    public record Summary(String assessmentId, String assessmentName, String templateName, int position,
            String timetableId, String title, String status, LocalDate startDate, LocalDate endDate,
            int paperCount) {
    }
}
