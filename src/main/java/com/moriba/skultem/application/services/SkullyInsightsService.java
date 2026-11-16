package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.GenerateStudentsRequiringAttentionUseCase;
import com.moriba.skultem.application.usecase.GetFeeDashboardSummaryUseCase;
import com.moriba.skultem.application.usecase.GetOutstandingFeesReportUseCase;
import com.moriba.skultem.application.usecase.ListClassesNeedingAttentionUseCase;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Skully's insights brief for school leadership: the school's own reports (students and classes needing attention,
 * attendance this week, fees collected and owed) gathered into facts, written up by Skully (Gemini, in the Worker)
 * as what is going well, what needs attention and what to do this week. The latest brief is kept per school and
 * written again when someone on the Skully page asks for it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkullyInsightsService {

    // Gemini on the free tier, and the facts barely change minute to minute.
    private static final Duration MIN_REFRESH = Duration.ofMinutes(10);
    private static final int TOP_STUDENTS = 15;
    private static final int TOP_ROWS = 6;

    /** The parts of the facts; who may see each is decided by the caller (see SkullyStaffService). */
    public static final String ATTENDANCE = "attendance", ACADEMICS = "academics", FEES = "fees";
    public static final Set<String> ALL_PARTS = Set.of(ATTENDANCE, ACADEMICS, FEES);

    private final JdbcTemplate jdbc;
    private final ModuleAccessService modules;
    private final GenerateStudentsRequiringAttentionUseCase studentsAttention;
    private final ListClassesNeedingAttentionUseCase classesAttention;
    private final GetFeeDashboardSummaryUseCase feeSummary;
    private final GetOutstandingFeesReportUseCase outstandingFees;
    private final SectionScopeService sections;
    // Our own, like WebPushService and MComposeClient: Spring Boot 4 provides Jackson 3, not this ObjectMapper bean.
    private final ObjectMapper json = new ObjectMapper();

    @Value("${skully.api-key:}")
    private String apiKey;

    @Value("${skully.worker-url:}")
    private String workerUrl;

    @Value("${skully.currency-prefix:Le }")
    private String currency;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** allowed: false for a section-limited leader - the brief covers the whole school. */
    public record Insights(boolean enabled, boolean configured, boolean allowed, String brief, Instant generatedAt,
            String generatedBy) {
    }

    public Insights view(String schoolId) {
        var rows = jdbc.queryForList("SELECT brief, generated_at, generated_by FROM skully_insights WHERE school_id = ?",
                schoolId);
        var row = rows.isEmpty() || !wholeSchool() ? null : rows.get(0);
        return new Insights(modules.isEnabled(schoolId, FeatureModule.WHATSAPP_ASSISTANT), configured(), wholeSchool(),
                row == null ? null : (String) row.get("brief"),
                row == null ? null : ((Timestamp) row.get("generated_at")).toInstant(),
                row == null ? null : (String) row.get("generated_by"));
    }

    public Insights refresh(String schoolId, String userId) {
        if (!modules.isEnabled(schoolId, FeatureModule.WHATSAPP_ASSISTANT)) {
            throw new RuleException("Skully isn't switched on for your school yet. Contact Skultem to get it.");
        }
        if (!configured()) throw new RuleException("Skully insights aren't set up yet. Contact Skultem.");
        if (!wholeSchool()) throw new RuleException("The brief covers the whole school, so only whole-school leaders can write it.");
        Insights current = view(schoolId);
        if (current.generatedAt() != null && current.generatedAt().isAfter(Instant.now().minus(MIN_REFRESH))) {
            throw new RuleException("The brief was written a few minutes ago. Try again in a little while.");
        }

        String brief = write(facts(schoolId, ALL_PARTS));
        String requestedBy = userId == null ? null : jdbc.queryForList(
                "SELECT trim(given_name || ' ' || family_name) FROM users WHERE id = ?", String.class, userId)
                .stream().findFirst().orElse(null);
        jdbc.update("""
                INSERT INTO skully_insights (school_id, brief, generated_at, generated_by) VALUES (?, ?, ?, ?)
                ON CONFLICT (school_id) DO UPDATE SET brief = EXCLUDED.brief, generated_at = EXCLUDED.generated_at,
                    generated_by = EXCLUDED.generated_by
                """, schoolId, brief, Timestamp.from(Instant.now()), requestedBy);
        return view(schoolId);
    }

    // The signed-in user sees the whole school (not one management section).
    private boolean wholeSchool() {
        return sections.restrictedLevels() == null;
    }

    private boolean configured() {
        return apiKey != null && !apiKey.isBlank() && workerUrl != null && !workerUrl.isBlank();
    }

    // ---------------------------------------------------------------------------------------------------------
    // The facts: only numbers the school's own reports already show leadership, limited to the parts asked for and
    // to the caller's management sections (the reports apply the signed-in user's scope; attendance does it here).
    // Each part is optional - a report that fails (e.g. no fees set up) is left out rather than failing the brief.

    public Map<String, Object> facts(String schoolId, Set<String> parts) {
        LocalDate today = LocalDate.now(SchoolTimeZone.ZONE);
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("school", jdbc.queryForObject("SELECT name FROM schools WHERE id = ?", String.class, schoolId));
        facts.put("today", today.toString());
        facts.put("currency", currency.trim());
        Map<String, Object> term = currentTerm(schoolId);
        String termId = term == null ? null : (String) term.get("id");
        facts.put("currentTerm", term == null ? null
                : Map.of("name", term.get("name"), "academicYear", term.get("academic_year"),
                        "startDate", term.get("start_date").toString()));

        if (parts.contains(ATTENDANCE)) part(facts, "attendance", () -> attendance(schoolId, today, term));
        if (parts.contains(ACADEMICS) && termId != null) {
            part(facts, "assessmentsThisTerm", () -> assessments(schoolId, termId));
        }
        if (parts.contains(ACADEMICS)) part(facts, "studentsNeedingAttention", () -> {
            var res = studentsAttention.execute(schoolId, null, null, null, null, 0, TOP_STUDENTS);
            List<Map<String, Object>> students = new ArrayList<>();
            for (var s : res.students()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", (Optional.ofNullable(s.givenNames()).orElse("") + " "
                        + Optional.ofNullable(s.familyName()).orElse("")).trim());
                m.put("academicAverage", s.academicAverage());
                m.put("attendanceRate", s.attendanceRate());
                m.put("assessmentsMissing", s.assessmentsMissing());
                m.put("trend", s.trend());
                List<String> flags = new ArrayList<>();
                if (s.lowAcademicSignal()) flags.add("low average");
                if (s.lowAttendanceSignal()) flags.add("low attendance");
                if (s.missingAssessmentsSignal()) flags.add("missing assessments");
                if (s.decliningTrendSignal()) flags.add("declining");
                m.put("flags", flags);
                students.add(m);
            }
            return Map.of("totalStudents", res.totalStudents(), "flagged", res.flaggedCount(),
                    "mostUrgent", students);
        });
        if (parts.contains(ACADEMICS)) part(facts, "classesNeedingAttention", () -> {
            var res = classesAttention.execute(schoolId, null);
            return Map.of("totalClasses", res.totalClasses(), "flaggedClasses", res.flaggedClasses(),
                    "flaggedStudents", res.flaggedStudents(),
                    "classes", res.classes().stream().limit(TOP_ROWS).map(c -> Map.of(
                            "class", (c.className() + " " + Optional.ofNullable(c.sectionName()).orElse("")).trim(),
                            "flagged", c.flaggedCount(), "students", c.totalStudents())).toList());
        });
        if (parts.contains(FEES)) part(facts, "fees", () -> {
            // This term's fees (the whole year between terms), plus the year's total still owed for context.
            var sum = feeSummary.execute(schoolId, null, termId);
            var owed = outstandingFees.execute(schoolId, null, termId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("academicYear", sum.academicYearName());
            m.put("period", term == null ? "whole academic year" : String.valueOf(term.get("name")));
            // The whole academic year (every term) - what "total fees" means.
            var year = termId == null ? sum : feeSummary.execute(schoolId, null, null);
            m.put("wholeYear", Map.of("expected", money(year.totalExpected()),
                    "collected", money(year.totalCollected()), "outstanding", money(year.totalOutstanding()),
                    "collectionRatePercent", year.collectionRate()));
            m.put("expected", money(sum.totalExpected()));
            m.put("collected", money(sum.totalCollected()));
            m.put("outstanding", money(sum.totalOutstanding()));
            m.put("collectionRatePercent", sum.collectionRate()); // already 0-100
            m.put("students", sum.totalStudents());
            m.put("fullyPaid", sum.studentsFullyPaid());
            m.put("withBalance", sum.studentsWithBalance());
            m.put("noPaymentYet", sum.studentsNoPayment());
            m.put("mostOwedByClass", owed.byClass().stream()
                    .sorted((a, b) -> b.outstanding().compareTo(a.outstanding())).limit(TOP_ROWS)
                    .map(c -> Map.of("class", c.className(), "outstanding", money(c.outstanding()),
                            "students", c.studentsWithOutstanding())).toList());
            m.put("mostOwedByFee", owed.byFeeType().stream()
                    .sorted((a, b) -> b.outstanding().compareTo(a.outstanding())).limit(TOP_ROWS)
                    .map(f -> Map.of("fee", Optional.ofNullable(f.feeCategoryName()).orElse("Other"),
                            "outstanding", money(f.outstanding()), "students", f.studentsWithOutstanding())).toList());
            return m;
        });
        return facts;
    }

    /** The active term of the school's active academic year (id, name, academic_year, start_date), or null between terms. */
    public Map<String, Object> currentTerm(String schoolId) {
        return jdbc.queryForList("""
                SELECT t.id, t.name, ay.name AS academic_year, t.start_date
                FROM terms t JOIN academic_years ay ON ay.id = t.academic_year_id AND ay.active
                WHERE t.school_id = ? AND t.status = 'ACTIVE'
                ORDER BY t.start_date DESC LIMIT 1
                """, schoolId).stream().findFirst().orElse(null);
    }

    /** A lifecycle status as Skully says it. */
    public static String stage(String status) {
        return switch (status) {
            case "DRAFT" -> "not submitted yet";
            case "RETURNED" -> "returned to the teacher for corrections";
            case "SUBMITTED" -> "submitted, waiting for approval";
            default -> "approved";
        };
    }

    // Present rate this week, last week and the term so far (school days that were taken, holidays excluded).
    private Map<String, Object> attendance(String schoolId, LocalDate today, Map<String, Object> term) {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        Collection<Level> levels = sections.restrictedLevels();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("thisWeekPresentPercent", rate(schoolId, monday, today, levels));
        m.put("lastWeekPresentPercent", rate(schoolId, monday.minusWeeks(1), monday.minusDays(1), levels));
        if (term != null) {
            m.put("termSoFarPresentPercent",
                    rate(schoolId, ((Date) term.get("start_date")).toLocalDate(), today, levels));
        }
        return m;
    }

    // This term's assessments by stage (approved, waiting for approval, not submitted...), and the class subjects
    // whose marks are not in yet - within the caller's sections.
    private Map<String, Object> assessments(String schoolId, String termId) {
        Collection<Level> levels = sections.restrictedLevels();
        if (levels != null && levels.isEmpty()) return Map.of();
        List<Object> args = new ArrayList<>(List.of(schoolId, termId));
        String scope = "";
        if (levels != null) {
            scope = " AND c.level IN (" + String.join(",", levels.stream().map(l -> "?").toList()) + ")";
            levels.forEach(l -> args.add(l.name()));
        }
        var rows = jdbc.queryForList("""
                SELECT lc.status, s.name AS subject, a.name AS assessment, c.name AS class_name, sec.name AS section_name,
                       trim(coalesce(u.given_name, '') || ' ' || coalesce(u.family_name, '')) AS teacher
                FROM class_subject_assessment_life_cycle lc
                JOIN teacher_subjects ts ON ts.id = lc.teacher_subject_id
                JOIN class_sessions cs ON cs.id = ts.class_session_id
                JOIN classes c ON c.id = cs.class_id
                LEFT JOIN sections sec ON sec.id = cs.section_id
                JOIN subjects s ON s.id = ts.subject_id
                JOIN assessments a ON a.id = lc.assessment_id
                LEFT JOIN teachers t ON t.id = ts.teacher_id
                LEFT JOIN users u ON u.id = t.user_id
                WHERE lc.school_id = ? AND lc.term_id = ?""" + scope + """

                ORDER BY c.name, sec.name, s.name, a.position
                """, args.toArray());
        Map<String, Integer> byStage = new LinkedHashMap<>();
        List<Map<String, Object>> notIn = new ArrayList<>();
        for (var r : rows) {
            String stage = stage((String) r.get("status"));
            byStage.merge(stage, 1, Integer::sum);
            if (!"approved".equals(stage) && notIn.size() < 20) {
                notIn.add(Map.of("class", (r.get("class_name") + " " + Optional.ofNullable(r.get("section_name")).orElse("")).trim(),
                        "subject", r.get("subject"), "assessment", r.get("assessment"), "stage", stage,
                        "teacher", Optional.ofNullable(r.get("teacher")).orElse("")));
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", rows.size());
        m.put("byStage", byStage);
        m.put("notReleasedYet", notIn);
        return m;
    }

    // levels: null for the whole school, else only classes at those levels (a section-limited caller).
    private Long rate(String schoolId, LocalDate from, LocalDate to, Collection<Level> levels) {
        List<Object> args = new ArrayList<>(List.of(schoolId, Date.valueOf(from), Date.valueOf(to)));
        String scope = "";
        if (levels != null) {
            if (levels.isEmpty()) return null;
            scope = " AND a.enrollment_id IN (SELECT e.id FROM enrollments e JOIN classes c ON c.id = e.class_id WHERE c.level IN ("
                    + String.join(",", levels.stream().map(l -> "?").toList()) + "))";
            levels.forEach(l -> args.add(l.name()));
        }
        Double r = jdbc.queryForObject("""
                SELECT 100.0 * COUNT(*) FILTER (WHERE a.present) / NULLIF(COUNT(*), 0)
                FROM attendance a WHERE a.school_id = ? AND a.date BETWEEN ? AND ? AND NOT a.holiday""" + scope,
                Double.class, args.toArray());
        return r == null ? null : Math.round(r);
    }

    private interface Part {
        Object get();
    }

    private void part(Map<String, Object> facts, String name, Part part) {
        try {
            facts.put(name, part.get());
        } catch (RuntimeException e) {
            log.warn("Skully insights: {} left out: {}", name, e.getMessage());
        }
    }

    private String money(BigDecimal value) {
        return currency + new java.text.DecimalFormat("#,##0.##").format(value == null ? BigDecimal.ZERO : value);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Skully writes the brief (POST {worker}/insights, same key as the Worker uses to call Skultem).

    private String write(Map<String, Object> facts) {
        try {
            var req = HttpRequest.newBuilder(URI.create(workerUrl.replaceAll("/+$", "") + "/insights"))
                    .timeout(Duration.ofSeconds(40))
                    .header("content-type", "application/json")
                    .header("X-Skully-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("facts", facts))))
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.error("Skully insights answered {}: {}", res.statusCode(),
                        res.body().substring(0, Math.min(300, res.body().length())));
                throw new RuleException("Skully couldn't write the brief just now. Please try again in a few minutes.");
            }
            String brief = json.readTree(res.body()).path("brief").asText("").trim();
            if (brief.isEmpty()) throw new RuleException("Skully couldn't write the brief just now. Please try again.");
            return brief;
        } catch (RuleException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleException("Skully couldn't write the brief just now. Please try again.");
        } catch (Exception e) {
            log.error("Skully insights failed: {}", e.getMessage(), e);
            throw new RuleException("Skully couldn't be reached. Please try again in a few minutes.");
        }
    }
}
