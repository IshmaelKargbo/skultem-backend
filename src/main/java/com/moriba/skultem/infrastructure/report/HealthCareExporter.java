package com.moriba.skultem.infrastructure.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.moriba.skultem.application.dto.HealthCareReportDTOs.Count;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.FrequentVisitor;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Limits;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.StudentHistory;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Summary;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.TrendRow;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Trends;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.TurnedAway;
import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.services.HealthCareService;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.shared.SchoolTimeZone;

/**
 * CSV / PDF exports of the Health Care reports (docs/HEALTH_CARE.md §10). They carry medical details, so only the
 * roles allowed to export reach them (H-4).
 */
@Component
public class HealthCareExporter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(SchoolTimeZone.ZONE);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm").withZone(SchoolTimeZone.ZONE);

    public ReportFile dailyLog(LocalDate date, List<HealthCareVisitDTO> visits, String format) {
        List<String> headers = List.of("In", "Out", "Student", "Class", "Complaint", "Treatment", "Medication",
                "Outcome", "Seen by");
        List<List<String>> rows = visits.stream().map(v -> List.of(
                clock(v.arrivedAt()), v.leftAt() == null ? (v.status().equals("OPEN") ? "Still in" : "Not recorded")
                        : clock(v.leftAt()),
                v.studentName(), text(v.studentClass()), complaints(v), text(v.treatment()), medication(v),
                outcome(v), text(v.openedByName())))
                .toList();
        return build("health-care-daily-log", "Health Care Daily Log - " + date, headers, rows, format);
    }

    public ReportFile summary(Summary s, String format) {
        List<String> headers = List.of("Group", "Item", "Visits");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Total", "Visits (not voided)", String.valueOf(s.visits())));
        rows.add(List.of("Total", "Students seen", String.valueOf(s.students())));
        rows.add(List.of("Total", "Urgent", String.valueOf(s.urgent())));
        rows.add(List.of("Total", "Over a limit", String.valueOf(s.overLimit())));
        rows.add(List.of("Total", "Turned away by a limit", String.valueOf(s.turnedAway())));
        if (s.averageMinutes() != null) {
            rows.add(List.of("Total", "Average minutes in the sick bay", String.valueOf(s.averageMinutes())));
        }
        addCounts(rows, "Outcome", s.byOutcome());
        addCounts(rows, "Complaint", s.byComplaint());
        for (var c : s.classes()) {
            rows.add(List.of("Class", c.className(), c.visits() + " (" + c.students() + " students, " + c.sentHome()
                    + " sent home, " + c.referred() + " referred)"));
        }
        return build("health-care-summary", "Health Care Visits " + s.from() + " to " + s.to(), headers, rows, format);
    }

    /** One table: a section and item per row, a column per bucket, then the total. */
    public ReportFile trends(Trends t, String format) {
        List<String> headers = new ArrayList<>(List.of("Section", "Item"));
        t.buckets().forEach(b -> headers.add(b.label()));
        headers.add("Total");
        List<List<String>> rows = new ArrayList<>();
        addTrendRows(rows, "Overview", t.overview());
        addTrendRows(rows, "Complaint", t.complaints());
        addTrendRows(rows, "Outcome", t.outcomes());
        addTrendRows(rows, "Class", t.classes());
        int blanks = t.buckets().size();
        for (var m : t.medicines()) {
            List<String> r = new ArrayList<>(List.of("Medicine", m.name() + " (" + m.students() + " students)"));
            for (int i = 0; i < blanks; i++) {
                r.add("");
            }
            r.add(String.valueOf(m.times()));
            rows.add(r);
        }
        return build("health-care-trends", "Health Care Trends - " + t.title(), headers, rows, format);
    }

    private static void addTrendRows(List<List<String>> rows, String section, List<TrendRow> trendRows) {
        for (var r : trendRows) {
            List<String> row = new ArrayList<>(List.of(section, r.label()));
            r.counts().forEach(c -> row.add(String.valueOf(c)));
            row.add(String.valueOf(r.total()));
            rows.add(row);
        }
    }

    public ReportFile frequent(LocalDate from, LocalDate to, List<FrequentVisitor> list, String format) {
        List<String> headers = List.of("Student", "Class", "Visits", "Dates", "Complaints");
        List<List<String>> rows = list.stream().map(f -> List.of(f.studentName(), text(f.studentClass()),
                String.valueOf(f.visits()), f.dates().stream().map(HealthCareExporter::time).collect(Collectors.joining("; ")),
                String.join(", ", f.complaints()))).toList();
        return build("health-care-frequent-visitors", "Frequent Sick Bay Visitors " + from + " to " + to, headers, rows,
                format);
    }

    public ReportFile limits(Limits l, String format) {
        List<String> headers = List.of("Record", "Date", "Student", "Class", "Rule", "Visits before", "Detail");
        List<List<String>> rows = new ArrayList<>();
        for (var v : l.overLimit()) {
            var o = v.overLimit();
            rows.add(List.of("Over limit", time(v.arrivedAt()), v.studentName(), text(v.studentClass()),
                    o.ruleName() + " (" + o.maxVisits() + " per " + o.period().toLowerCase() + ")",
                    String.valueOf(o.count()),
                    o.urgent() ? "Urgent complaint" : o.reason() != null ? "Reason: " + o.reason() : "Warned"));
        }
        for (TurnedAway t : l.turnedAway()) {
            rows.add(List.of("Turned away", time(t.at()), t.studentName(), text(t.studentClass()),
                    t.ruleName() + " (" + t.maxVisits() + " per " + t.period().toLowerCase() + ")",
                    String.valueOf(t.visitCount()), t.complaints()));
        }
        return build("health-care-limits", "Health Care Limits " + l.from() + " to " + l.to(), headers, rows, format);
    }

    public ReportFile student(StudentHistory h, String format) {
        List<String> headers = List.of("Date", "Class", "Complaint", "Treatment", "Medication", "Outcome", "Status");
        List<List<String>> rows = new ArrayList<>();
        for (var v : h.visits()) {
            rows.add(List.of(time(v.arrivedAt()), text(v.studentClass()), complaints(v), text(v.treatment()),
                    medication(v), outcome(v), status(v)));
        }
        for (var t : h.turnedAway()) {
            rows.add(List.of(time(t.at()), text(t.studentClass()), t.complaints(), "", "", "",
                    "Turned away (" + t.ruleName() + ")"));
        }
        return build("health-care-history", "Health Care Record - " + h.studentName(), headers, rows, format);
    }

    private static void addCounts(List<List<String>> rows, String group, List<Count> counts) {
        for (var c : counts) {
            rows.add(List.of(group, c.label(), String.valueOf(c.count())));
        }
    }

    private static String complaints(HealthCareVisitDTO v) {
        String text = String.join(", ", v.complaints().stream().map(HealthCareVisit.Complaint::label).toList());
        return v.complaintOther() == null ? text : text + " (" + v.complaintOther() + ")";
    }

    private static String medication(HealthCareVisitDTO v) {
        return v.medications().stream().map(m -> m.name() + " " + m.dose() + " at " + clock(m.givenAt()))
                .collect(Collectors.joining("; "));
    }

    private static String outcome(HealthCareVisitDTO v) {
        if (v.outcome() == null) {
            return "";
        }
        String label = HealthCareService.outcomeLabel(HealthCareVisit.Outcome.valueOf(v.outcome()));
        if (v.referredTo() != null) {
            label += " - " + v.referredTo();
        }
        if (v.collectedBy() != null) {
            label += " - collected by " + v.collectedBy();
        }
        return label;
    }

    private static String status(HealthCareVisitDTO v) {
        return switch (v.status()) {
            case "OPEN" -> v.needsFollowUp() ? "Needs follow-up" : "In the sick bay";
            case "CLOSED" -> v.followUpReason() == null ? "Closed" : "Followed up";
            case "VOIDED" -> "Voided - " + text(v.voidReason());
            default -> v.status();
        };
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

    private static String clock(Instant instant) {
        return instant == null ? "" : CLOCK.format(instant);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
