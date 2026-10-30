package com.moriba.skultem.infrastructure.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.moriba.skultem.application.dto.LibraryAttendanceTotalsDTO;
import com.moriba.skultem.application.dto.LibrarySessionDTO;
import com.moriba.skultem.application.dto.LibraryStudentHistoryDTO;
import com.moriba.skultem.application.dto.LibraryVisitDTO;
import com.moriba.skultem.domain.shared.SchoolTimeZone;

/**
 * CSV / PDF exports of the Library Attendance reports (R-3), in the same formats as ReportExportService.
 * Visits and scheduled sessions are separate files, or separate sections of the student report.
 */
@Component
public class LibraryAttendanceExporter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(SchoolTimeZone.ZONE);

    public ReportFile visits(List<LibraryVisitDTO> visits, String format) {
        List<String> headers = List.of("Student", "Class", "Checked in", "Checked out", "Minutes", "State",
                "Checked in by", "Follow-up");
        List<List<String>> rows = visits.stream().map(v -> List.of(
                v.studentName(), text(v.studentClass()), time(v.checkedInAt()), time(v.checkedOutAt()),
                v.durationMinutes() == null ? "" : v.durationMinutes().toString(), visitState(v),
                text(v.checkedInByName()),
                v.resolvedAt() == null ? "" : "Check-out not recorded - " + text(v.resolutionReason()) + " ("
                        + text(v.resolvedByName()) + ")"))
                .toList();
        return build("library-visits", "Library Visits", headers, rows, format);
    }

    public ReportFile sessions(List<LibrarySessionDTO> sessions, String format) {
        List<String> headers = List.of("Date", "Class", "Period", "Time", "Status", "On roster", "Present", "Absent",
                "Excused", "Unmarked");
        List<List<String>> rows = sessions.stream().map(s -> List.of(
                s.date().toString(), text(s.className()), text(s.periodName()),
                s.startTime() + " - " + s.endTime(), sessionState(s),
                s.id() == null ? "" : String.valueOf(s.rosterCount()), String.valueOf(s.present()),
                String.valueOf(s.absent()), String.valueOf(s.excused()), String.valueOf(s.unmarked())))
                .toList();
        return build("library-sessions", "Scheduled Library Attendance", headers, rows, format);
    }

    public ReportFile totals(LibraryAttendanceTotalsDTO totals, String format) {
        List<String> headers = List.of("Class", "Finalized sessions", "Present", "Absent", "Excused");
        List<List<String>> rows = new ArrayList<>();
        for (var r : totals.classes()) {
            rows.add(totalRow(r));
        }
        rows.add(totalRow(totals.overall()));
        rows.add(List.of("Not counted: " + totals.incompleteSessions() + " incomplete, " + totals.cancelledSessions()
                + " cancelled", "", "", "", ""));
        return build("library-attendance-totals",
                "Library Attendance Totals " + totals.from() + " to " + totals.to(), headers, rows, format);
    }

    public ReportFile student(LibraryStudentHistoryDTO history, String format) {
        List<String> headers = List.of("Record", "Date", "Class", "Detail", "Status");
        List<List<String>> rows = new ArrayList<>();
        for (var v : history.visits()) {
            rows.add(List.of("Independent visit", time(v.checkedInAt()), text(v.studentClass()),
                    v.checkedOutAt() == null ? "No check-out" : "Out " + time(v.checkedOutAt()), visitState(v)));
        }
        for (var s : history.sessions()) {
            rows.add(List.of("Scheduled session", s.date().toString(), text(s.className()), text(s.periodName()),
                    s.status() + ("CANCELLED".equals(s.sessionStatus()) ? " (session cancelled)"
                            : "IN_PROGRESS".equals(s.sessionStatus()) ? " (not finalized)" : "")
                            + (s.corrected() ? " (corrected)" : "")));
        }
        return build("library-history", "Library Record - " + history.studentName(), headers, rows, format);
    }

    private static List<String> totalRow(LibraryAttendanceTotalsDTO.Row r) {
        return List.of(text(r.className()), String.valueOf(r.sessions()), String.valueOf(r.present()),
                String.valueOf(r.absent()), String.valueOf(r.excused()));
    }

    private static String visitState(LibraryVisitDTO v) {
        return switch (v.status()) {
            case "OPEN" -> v.unresolved() ? "Not checked out" : "In the library";
            case "CHECKED_OUT" -> "Checked out";
            case "RESOLVED" -> "Followed up";
            default -> v.status();
        };
    }

    private static String sessionState(LibrarySessionDTO s) {
        String label = switch (s.status()) {
            case "UPCOMING" -> "Upcoming";
            case "IN_PROGRESS" -> "In progress";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            default -> s.status();
        };
        return s.incomplete() ? label + " (incomplete)" : label;
    }

    private static ReportFile build(String baseName, String title, List<String> headers, List<List<String>> rows,
            String format) {
        boolean pdf = "pdf".equalsIgnoreCase(format == null ? "" : format.trim());
        byte[] data = pdf ? ReportExportUtil.toPdf(title, headers, rows) : ReportExportUtil.toCsv(headers, rows);
        return new ReportFile(baseName + "-" + LocalDate.now(SchoolTimeZone.ZONE) + (pdf ? ".pdf" : ".csv"),
                pdf ? "application/pdf" : "text/csv", data);
    }

    private static String time(Instant instant) {
        return instant == null ? "" : TIME.format(instant);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
