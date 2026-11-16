package com.moriba.skultem.application.services;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.FeatureModule;

import lombok.RequiredArgsConstructor;

/**
 * Weekly plans: a class section's school term split into its weeks (Monday to Friday), each school day with one
 * subject per session - e.g. two a day for a nursery class ("Morning", "After break"). Generic for any level.
 * <p>
 * Auto-fill goes through the class's subjects in turn over every school day of the term (school holidays left
 * out); staff then change any day by hand and publish. Parents (portal and Skully) only see published plans.
 * <p>
 * A plan can instead repeat (cycleWeeks > 0): the school plans a few weeks by weekday - e.g. a 4-week month for
 * nursery - and that pattern is written onto every school day of the term, so parents and Skully read it like
 * any other plan.
 */
@Service
@RequiredArgsConstructor
public class WeeklyPlanService {

    public static final int MAX_SLOTS = 6;
    public static final int MAX_CYCLE_WEEKS = 8;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final List<Slot> DEFAULT_SLOTS = List.of(new Slot("Morning", "08:30", "10:00"),
            new Slot("After break", "10:30", "12:00"));

    private final JdbcTemplate jdbc;
    private final ModuleAccessService modules;
    // Our own: Spring Boot 4 provides Jackson 3, not this ObjectMapper bean (see SkullyInsightsService).
    private final ObjectMapper json = new ObjectMapper();

    /** One session of the day; start/end "HH:mm" (24-hour) or empty. */
    public record Slot(String name, String start, String end) {
    }

    public record Subject(String id, String name) {
    }

    public record Entry(int slot, String subjectId, String subjectName) {
    }

    public record PlanDay(String date, String weekday, String holiday, List<Entry> entries) {
    }

    public record PlanWeek(String label, String start, String end, List<PlanDay> days) {
    }

    /**
     * slotsSource: CLASS when the class has its own sessions, SCHOOL when it uses the school's. cycleWeeks: how many
     * weeks repeat (0 = every week planned on its own); cycle: the repeating pattern (empty when not repeating).
     */
    public record Plan(String id, String classSessionId, String className, String termId, String termName,
            String termStart, String termEnd, String status, List<Slot> slots, String slotsSource, List<Subject> subjects,
            List<PlanWeek> weeks, int cycleWeeks, List<CycleEntry> cycle) {
    }

    /** One cell of a repeating plan: week of the cycle (1..), weekday (1 Monday .. 5 Friday), session slot. */
    public record CycleEntry(int week, int weekday, int slot, String subjectId, String subjectName) {
    }

    public record CycleInput(int week, int weekday, int slot, String subjectId) {
    }

    // A school term: the plan runs from its first day to its last.
    private record Term(String id, String name, LocalDate start, LocalDate end) {
    }

    /** The sessions that apply to a class, and whether they are its own (CLASS) or the school's (SCHOOL). */
    public record ClassSlots(List<Slot> slots, String source) {
    }

    public record EntryInput(String date, int slot, String subjectId) {
    }

    // ---------------------------------------------------------------------------------------------------------
    // The school's session slots

    /** The school's sessions: what every class uses unless it has its own. */
    public List<Slot> slots(String schoolId) {
        var rows = jdbc.queryForList("SELECT slots FROM weekly_plan_settings WHERE school_id = ?", String.class, schoolId);
        List<Slot> slots = rows.isEmpty() ? null : parse(rows.get(0));
        return slots == null ? DEFAULT_SLOTS : slots;
    }

    /** The sessions for this class section: its class's own, else the school's. */
    public ClassSlots slotsFor(String schoolId, String classSessionId) {
        String classId = classId(schoolId, classSessionId);
        var rows = jdbc.queryForList("SELECT slots FROM weekly_plan_class_slots WHERE class_id = ? AND school_id = ?",
                String.class, classId, schoolId);
        List<Slot> own = rows.isEmpty() ? null : parse(rows.get(0));
        return own == null ? new ClassSlots(slots(schoolId), "SCHOOL") : new ClassSlots(own, "CLASS");
    }

    /** Gives the class (of this section) its own sessions; every section of the class uses them. */
    @Transactional
    public ClassSlots saveClassSlots(String schoolId, String classSessionId, List<Slot> slots) {
        String classId = classId(schoolId, classSessionId);
        jdbc.update("""
                INSERT INTO weekly_plan_class_slots (class_id, school_id, slots, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (class_id) DO UPDATE SET slots = EXCLUDED.slots, updated_at = EXCLUDED.updated_at
                """, classId, schoolId, write(validate(slots)), Timestamp.from(Instant.now()));
        return slotsFor(schoolId, classSessionId);
    }

    /** The class goes back to the school's sessions. */
    @Transactional
    public ClassSlots clearClassSlots(String schoolId, String classSessionId) {
        jdbc.update("DELETE FROM weekly_plan_class_slots WHERE class_id = ? AND school_id = ?",
                classId(schoolId, classSessionId), schoolId);
        return slotsFor(schoolId, classSessionId);
    }

    private List<Slot> parse(String text) {
        try {
            List<Slot> slots = json.readValue(text, new TypeReference<List<Slot>>() {
            });
            return slots.isEmpty() ? null : slots;
        } catch (Exception e) {
            return null;
        }
    }

    private String write(List<Slot> slots) {
        try {
            return json.writeValueAsString(slots);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String classId(String schoolId, String classSessionId) {
        var rows = jdbc.queryForList("SELECT class_id FROM class_sessions WHERE id = ? AND school_id = ?", String.class,
                classSessionId, schoolId);
        if (rows.isEmpty()) throw new NotFoundException("Class not found");
        return rows.get(0);
    }

    @Transactional
    public List<Slot> saveSlots(String schoolId, List<Slot> slots) {
        jdbc.update("""
                INSERT INTO weekly_plan_settings (school_id, slots, updated_at) VALUES (?, ?, ?)
                ON CONFLICT (school_id) DO UPDATE SET slots = EXCLUDED.slots, updated_at = EXCLUDED.updated_at
                """, schoolId, write(validate(slots)), Timestamp.from(Instant.now()));
        return slots(schoolId);
    }

    static List<Slot> validate(List<Slot> slots) {
        if (slots == null || slots.isEmpty()) throw new RuleException("Add at least one session");
        if (slots.size() > MAX_SLOTS) throw new RuleException("At most " + MAX_SLOTS + " sessions a day");
        List<Slot> clean = new ArrayList<>();
        for (Slot s : slots) {
            String name = s.name() == null ? "" : s.name().trim();
            if (name.isEmpty()) throw new RuleException("Every session needs a name");
            String start = time(s.start()), end = time(s.end());
            if (start != null && end != null && end.compareTo(start) <= 0) {
                throw new RuleException(name + " must end after it starts");
            }
            clean.add(new Slot(name, start == null ? "" : start, end == null ? "" : end));
        }
        return clean;
    }

    private static String time(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.matches("\\d{2}:\\d{2}")) throw new RuleException("Times must look like 08:30");
        return value;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Plans (staff)

    public Plan plan(String schoolId, String classSessionId, String termId) {
        Term term = term(schoolId, termId);
        String className = className(schoolId, classSessionId);
        var row = planRow(classSessionId, term.id());
        String planId = row == null ? null : (String) row.get("id");
        String status = row == null ? "DRAFT" : (String) row.get("status");
        int cycleWeeks = row == null || row.get("cycle_weeks") == null ? 0 : ((Number) row.get("cycle_weeks")).intValue();
        ClassSlots slots = slotsFor(schoolId, classSessionId);
        return new Plan(planId, classSessionId, className, term.id(), term.name(), term.start().toString(),
                term.end().toString(), status, slots.slots(), slots.source(), subjects(schoolId, classSessionId),
                weeks(schoolId, term, entries(planId, slots.slots().size())), cycleWeeks,
                cycleWeeks > 0 ? cycleEntries(planId, cycleWeeks, slots.slots().size()) : List.of());
    }

    /** Fills every school day of the term, going through the class's subjects in turn. Replaces what was there. */
    @Transactional
    public Plan autoFill(String schoolId, String classSessionId, String termId) {
        return autoFill(schoolId, classSessionId, termId, 0);
    }

    /** As above; with cycleWeeks > 0 it fills that many weeks in turn and repeats them over the term instead. */
    @Transactional
    public Plan autoFill(String schoolId, String classSessionId, String termId, int cycleWeeks) {
        if (cycleWeeks > 0) {
            return autoFillCycle(schoolId, classSessionId, termId, cycleWeeks);
        }
        Term term = term(schoolId, termId);
        className(schoolId, classSessionId);
        List<Subject> subjects = subjects(schoolId, classSessionId);
        if (subjects.isEmpty()) {
            throw new RuleException("This class has no subjects yet - assign subjects to it first");
        }
        int perDay = slotsFor(schoolId, classSessionId).slots().size();
        String planId = ensurePlan(schoolId, classSessionId, term.id());
        setCycle(planId, 0);
        jdbc.update("DELETE FROM weekly_plan_entries WHERE plan_id = ?", planId);
        int next = 0;
        for (LocalDate day : schoolDays(schoolId, term.start(), term.end())) {
            // A day never repeats a subject when there are enough to go round.
            Set<String> today = new HashSet<>();
            for (int slot = 0; slot < perDay; slot++) {
                Subject s = subjects.get(next % subjects.size());
                if (subjects.size() >= perDay) {
                    while (today.contains(s.id())) s = subjects.get(++next % subjects.size());
                }
                today.add(s.id());
                insertEntry(planId, day, slot, s.id());
                next++;
            }
        }
        touch(planId);
        return plan(schoolId, classSessionId, term.id());
    }

    /** Saves the whole term as edited: every entry given replaces what was there. */
    @Transactional
    public Plan save(String schoolId, String classSessionId, String termId, List<EntryInput> entries) {
        Term term = term(schoolId, termId);
        className(schoolId, classSessionId);
        int perDay = slotsFor(schoolId, classSessionId).slots().size();
        Set<String> allowed = new HashSet<>(subjects(schoolId, classSessionId).stream().map(Subject::id).toList());
        Set<LocalDate> days = new HashSet<>(schoolDays(schoolId, term.start(), term.end()));
        String planId = ensurePlan(schoolId, classSessionId, term.id());
        // Planned week by week from now on (the days a repeating plan wrote come in as ordinary entries).
        setCycle(planId, 0);
        jdbc.update("DELETE FROM weekly_plan_entries WHERE plan_id = ?", planId);
        Set<String> seen = new HashSet<>();
        for (EntryInput e : entries == null ? List.<EntryInput>of() : entries) {
            if (e.subjectId() == null || e.subjectId().isBlank()) continue;
            LocalDate day;
            try {
                day = LocalDate.parse(e.date());
            } catch (DateTimeParseException | NullPointerException ex) {
                throw new RuleException("Unknown date " + e.date());
            }
            if (!days.contains(day)) throw new RuleException(day + " isn't a school day in " + term.name());
            if (e.slot() < 0 || e.slot() >= perDay) throw new RuleException("Unknown session on " + day);
            if (!allowed.contains(e.subjectId())) throw new RuleException("A subject on " + day + " isn't one of this class's");
            if (!seen.add(day + "|" + e.slot())) continue;
            insertEntry(planId, day, e.slot(), e.subjectId());
        }
        touch(planId);
        return plan(schoolId, classSessionId, term.id());
    }

    /**
     * Saves a repeating plan: the pattern for cycleWeeks weeks (by weekday), then writes it onto every school day of
     * the term - the term's week 1 takes the pattern's week 1, week 5 takes week 1 again (with 4), and so on.
     */
    @Transactional
    public Plan saveCycle(String schoolId, String classSessionId, String termId, int cycleWeeks, List<CycleInput> entries) {
        checkCycle(cycleWeeks);
        Term term = term(schoolId, termId);
        className(schoolId, classSessionId);
        int perDay = slotsFor(schoolId, classSessionId).slots().size();
        Set<String> allowed = new HashSet<>(subjects(schoolId, classSessionId).stream().map(Subject::id).toList());
        Map<String, String> pattern = new LinkedHashMap<>();
        for (CycleInput e : entries == null ? List.<CycleInput>of() : entries) {
            if (e.subjectId() == null || e.subjectId().isBlank()) continue;
            if (e.week() < 1 || e.week() > cycleWeeks) throw new RuleException("Week " + e.week() + " isn't in the plan");
            if (e.weekday() < 1 || e.weekday() > 5) throw new RuleException("Plans run Monday to Friday");
            if (e.slot() < 0 || e.slot() >= perDay) throw new RuleException("Unknown session in week " + e.week());
            if (!allowed.contains(e.subjectId())) {
                throw new RuleException("A subject in week " + e.week() + " isn't one of this class's");
            }
            pattern.putIfAbsent(key(e.week(), e.weekday(), e.slot()), e.subjectId());
        }
        String planId = ensurePlan(schoolId, classSessionId, term.id());
        writeCycle(schoolId, planId, term, cycleWeeks, pattern);
        return plan(schoolId, classSessionId, term.id());
    }

    // Auto-fill for a repeating plan: the class's subjects in turn over the cycle's weekdays, then spread over the term.
    private Plan autoFillCycle(String schoolId, String classSessionId, String termId, int cycleWeeks) {
        checkCycle(cycleWeeks);
        Term term = term(schoolId, termId);
        className(schoolId, classSessionId);
        List<Subject> subjects = subjects(schoolId, classSessionId);
        if (subjects.isEmpty()) {
            throw new RuleException("This class has no subjects yet - assign subjects to it first");
        }
        int perDay = slotsFor(schoolId, classSessionId).slots().size();
        Map<String, String> pattern = new LinkedHashMap<>();
        int next = 0;
        for (int week = 1; week <= cycleWeeks; week++) {
            for (int weekday = 1; weekday <= 5; weekday++) {
                Set<String> today = new HashSet<>();
                for (int slot = 0; slot < perDay; slot++) {
                    Subject s = subjects.get(next % subjects.size());
                    if (subjects.size() >= perDay) {
                        while (today.contains(s.id())) s = subjects.get(++next % subjects.size());
                    }
                    today.add(s.id());
                    pattern.put(key(week, weekday, slot), s.id());
                    next++;
                }
            }
        }
        String planId = ensurePlan(schoolId, classSessionId, term.id());
        writeCycle(schoolId, planId, term, cycleWeeks, pattern);
        return plan(schoolId, classSessionId, term.id());
    }

    private static void checkCycle(int cycleWeeks) {
        if (cycleWeeks < 1 || cycleWeeks > MAX_CYCLE_WEEKS) {
            throw new RuleException("A plan can repeat every 1 to " + MAX_CYCLE_WEEKS + " weeks");
        }
    }

    // Stores the pattern and rewrites the term's days from it.
    private void writeCycle(String schoolId, String planId, Term term, int cycleWeeks, Map<String, String> pattern) {
        setCycle(planId, cycleWeeks);
        jdbc.update("DELETE FROM weekly_plan_cycle_entries WHERE plan_id = ?", planId);
        for (var e : pattern.entrySet()) {
            String[] k = e.getKey().split("/");
            jdbc.update("INSERT INTO weekly_plan_cycle_entries (plan_id, week, weekday, slot, subject_id) VALUES (?, ?, ?, ?, ?)",
                    planId, Integer.parseInt(k[0]), Integer.parseInt(k[1]), Integer.parseInt(k[2]), e.getValue());
        }
        jdbc.update("DELETE FROM weekly_plan_entries WHERE plan_id = ?", planId);
        LocalDate firstMonday = term.start().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (LocalDate day : schoolDays(schoolId, term.start(), term.end())) {
            int week = cycleWeek(firstMonday, day, cycleWeeks);
            int weekday = day.getDayOfWeek().getValue();
            for (int slot = 0; slot < MAX_SLOTS; slot++) {
                String subjectId = pattern.get(key(week, weekday, slot));
                if (subjectId != null) insertEntry(planId, day, slot, subjectId);
            }
        }
        touch(planId);
    }

    /** Which week of the cycle a day falls in: the term's first week is week 1. */
    static int cycleWeek(LocalDate firstMonday, LocalDate day, int cycleWeeks) {
        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(firstMonday,
                day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        return (int) (weeks % cycleWeeks) + 1;
    }

    private static String key(int week, int weekday, int slot) {
        return week + "/" + weekday + "/" + slot;
    }

    private void setCycle(String planId, int cycleWeeks) {
        jdbc.update("UPDATE weekly_plans SET cycle_weeks = ? WHERE id = ?", cycleWeeks, planId);
        if (cycleWeeks == 0) jdbc.update("DELETE FROM weekly_plan_cycle_entries WHERE plan_id = ?", planId);
    }

    private List<CycleEntry> cycleEntries(String planId, int cycleWeeks, int perDay) {
        if (planId == null) return List.of();
        return jdbc.query("SELECT c.week, c.weekday, c.slot, s.id AS subject_id, s.name AS subject "
                + "FROM weekly_plan_cycle_entries c JOIN subjects s ON s.id = c.subject_id "
                + "WHERE c.plan_id = ? AND c.week <= ? AND c.slot < ? ORDER BY c.week, c.weekday, c.slot",
                (rs, n) -> new CycleEntry(rs.getInt("week"), rs.getInt("weekday"), rs.getInt("slot"),
                        rs.getString("subject_id"), rs.getString("subject")), planId, cycleWeeks, perDay);
    }

    @Transactional
    public Plan setStatus(String schoolId, String classSessionId, String termId, boolean published) {
        Term term = term(schoolId, termId);
        var row = planRow(classSessionId, term.id());
        if (row == null || !schoolId.equals(row.get("school_id"))) throw new NotFoundException("Save the plan first");
        if (published) {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM weekly_plan_entries WHERE plan_id = ?", Integer.class,
                    row.get("id"));
            if (n == null || n == 0) throw new RuleException("The plan is empty - auto-fill or add subjects first");
        }
        jdbc.update("UPDATE weekly_plans SET status = ?, updated_at = ? WHERE id = ?", published ? "PUBLISHED" : "DRAFT",
                Timestamp.from(Instant.now()), row.get("id"));
        return plan(schoolId, classSessionId, term.id());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Parents (portal and Skully): published plans only, this week and next

    /** Whether this parent user's child is this student (at this school). */
    public boolean isParentOf(String userId, String studentId, String schoolId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM students st JOIN parents p ON p.id = st.parent_id
                WHERE st.id = ? AND st.school_id = ? AND p.user_id = ? AND st.status = 'ACTIVE'
                """, Integer.class, studentId, schoolId, userId);
        return n != null && n > 0;
    }

    /** As below, for a student whose school isn't known yet (Skully). */
    public Optional<Map<String, Object>> forStudent(String studentId, LocalDate from) {
        var school = jdbc.queryForList("SELECT school_id FROM students WHERE id = ?", String.class, studentId);
        return school.isEmpty() ? Optional.empty() : forStudent(school.get(0), studentId, from);
    }

    /** The child's class plan for the two weeks from the week of `from`, as far as it is published. */
    public Optional<Map<String, Object>> forStudent(String schoolId, String studentId, LocalDate from) {
        if (!modules.isEnabled(schoolId, FeatureModule.TIMETABLE)) return Optional.empty();
        var session = jdbc.queryForList("SELECT session_id FROM students WHERE id = ? AND school_id = ?", String.class,
                studentId, schoolId);
        if (session.isEmpty() || session.get(0) == null) return Optional.empty();
        String classSessionId = session.get(0);
        LocalDate monday = from.with(DayOfWeek.MONDAY), friday = monday.plusWeeks(1).plusDays(4);
        List<Slot> slots = slotsFor(schoolId, classSessionId).slots();
        Map<String, String> holidays = holidays(schoolId, monday, friday);
        Map<LocalDate, List<Entry>> byDay = new HashMap<>();
        for (var r : jdbc.queryForList("""
                SELECT e.day, e.slot, s.id AS subject_id, s.name AS subject
                FROM weekly_plan_entries e
                JOIN weekly_plans p ON p.id = e.plan_id AND p.status = 'PUBLISHED'
                JOIN subjects s ON s.id = e.subject_id
                WHERE p.class_session_id = ? AND p.school_id = ? AND e.day BETWEEN ? AND ? AND e.slot < ?
                ORDER BY e.day, e.slot
                """, classSessionId, schoolId, Date.valueOf(monday), Date.valueOf(friday), slots.size())) {
            byDay.computeIfAbsent(((Date) r.get("day")).toLocalDate(), k -> new ArrayList<>())
                    .add(new Entry(((Number) r.get("slot")).intValue(), (String) r.get("subject_id"), (String) r.get("subject")));
        }
        if (byDay.isEmpty()) return Optional.empty();
        List<PlanWeek> weeks = new ArrayList<>();
        for (LocalDate wk = monday; !wk.isAfter(friday); wk = wk.plusWeeks(1)) {
            List<PlanDay> days = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                LocalDate d = wk.plusDays(i);
                days.add(new PlanDay(d.toString(), weekday(d), holidays.get(d.toString()), byDay.getOrDefault(d, List.of())));
            }
            weeks.add(new PlanWeek(label(wk, wk.plusDays(4)), wk.toString(), wk.plusDays(4).toString(), days));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("className", className(schoolId, classSessionId));
        out.put("slots", slots);
        out.put("weeks", weeks);
        return Optional.of(out);
    }

    // ---------------------------------------------------------------------------------------------------------

    // The term asked for, or - with none - the one running today (else the next one, else the last one).
    private Term term(String schoolId, String termId) {
        var rows = termId == null || termId.isBlank()
                ? jdbc.queryForList("""
                        SELECT id, name, start_date, end_date FROM terms WHERE school_id = ?
                        ORDER BY CASE WHEN ? BETWEEN start_date AND end_date THEN 0 WHEN start_date > ? THEN 1 ELSE 2 END,
                                 CASE WHEN start_date > ? THEN start_date END ASC, end_date DESC
                        LIMIT 1
                        """, schoolId, Date.valueOf(today()), Date.valueOf(today()), Date.valueOf(today()))
                : jdbc.queryForList("SELECT id, name, start_date, end_date FROM terms WHERE id = ? AND school_id = ?",
                        termId, schoolId);
        if (rows.isEmpty()) throw new NotFoundException("Term not found - set up the school's terms first");
        var r = rows.get(0);
        return new Term((String) r.get("id"), (String) r.get("name"), ((Date) r.get("start_date")).toLocalDate(),
                ((Date) r.get("end_date")).toLocalDate());
    }

    private String className(String schoolId, String classSessionId) {
        var rows = jdbc.queryForList("""
                SELECT c.name AS class_name, sec.name AS section, str.name AS stream
                FROM class_sessions cs JOIN classes c ON c.id = cs.class_id
                LEFT JOIN sections sec ON sec.id = cs.section_id LEFT JOIN streams str ON str.id = cs.stream_id
                WHERE cs.id = ? AND cs.school_id = ?
                """, classSessionId, schoolId);
        if (rows.isEmpty()) throw new NotFoundException("Class not found");
        var r = rows.get(0);
        StringBuilder s = new StringBuilder(String.valueOf(r.get("class_name")));
        for (String k : List.of("section", "stream")) {
            Object v = r.get(k);
            if (v != null && !v.toString().isBlank()) s.append(' ').append(v);
        }
        return s.toString();
    }

    // The subjects taught in this class section, or the class's curriculum when none are assigned yet.
    List<Subject> subjects(String schoolId, String classSessionId) {
        List<Subject> taught = jdbc.query("""
                SELECT DISTINCT s.id, s.name FROM teacher_subjects ts JOIN subjects s ON s.id = ts.subject_id
                WHERE ts.class_session_id = ? AND ts.school_id = ? ORDER BY s.name
                """, (rs, n) -> new Subject(rs.getString("id"), rs.getString("name")), classSessionId, schoolId);
        if (!taught.isEmpty()) return taught;
        return jdbc.query("""
                SELECT DISTINCT s.id, s.name FROM class_sessions cs
                JOIN class_subjects csub ON csub.class_id = cs.class_id
                JOIN subjects s ON s.id = csub.subject_id
                WHERE cs.id = ? AND cs.school_id = ? ORDER BY s.name
                """, (rs, n) -> new Subject(rs.getString("id"), rs.getString("name")), classSessionId, schoolId);
    }

    // Monday to Friday from `from` to `to`, without the school's holidays.
    List<LocalDate> schoolDays(String schoolId, LocalDate from, LocalDate to) {
        Map<String, String> holidays = holidays(schoolId, from, to);
        List<LocalDate> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (d.getDayOfWeek().getValue() <= 5 && !holidays.containsKey(d.toString())) out.add(d);
        }
        return out;
    }

    private Map<String, String> holidays(String schoolId, LocalDate from, LocalDate to) {
        Map<String, String> out = new HashMap<>();
        for (var r : jdbc.queryForList("SELECT date, name FROM holidays WHERE school_id = ? AND date BETWEEN ? AND ?",
                schoolId, Date.valueOf(from), Date.valueOf(to))) {
            out.put(((Date) r.get("date")).toLocalDate().toString(), (String) r.get("name"));
        }
        return out;
    }

    // The term as weeks (Monday to Friday), only the days inside the term; holidays shown, without subjects.
    private List<PlanWeek> weeks(String schoolId, Term term, Map<LocalDate, List<Entry>> entries) {
        Map<String, String> holidays = holidays(schoolId, term.start(), term.end());
        List<PlanWeek> weeks = new ArrayList<>();
        LocalDate monday = term.start().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (LocalDate wk = monday; !wk.isAfter(term.end()); wk = wk.plusWeeks(1)) {
            List<PlanDay> days = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                LocalDate d = wk.plusDays(i);
                if (d.isBefore(term.start()) || d.isAfter(term.end())) continue;
                days.add(new PlanDay(d.toString(), weekday(d), holidays.get(d.toString()), entries.getOrDefault(d, List.of())));
            }
            if (days.isEmpty()) continue;
            weeks.add(new PlanWeek("Week " + (weeks.size() + 1) + " · " + label(LocalDate.parse(days.get(0).date()),
                    LocalDate.parse(days.get(days.size() - 1).date())), days.get(0).date(), days.get(days.size() - 1).date(), days));
        }
        return weeks;
    }

    // Entries beyond the class's current number of sessions (it had more before) are left out, not deleted.
    private Map<LocalDate, List<Entry>> entries(String planId, int perDay) {
        Map<LocalDate, List<Entry>> out = new HashMap<>();
        if (planId == null) return out;
        for (var r : jdbc.queryForList("""
                SELECT e.day, e.slot, s.id AS subject_id, s.name AS subject FROM weekly_plan_entries e
                JOIN subjects s ON s.id = e.subject_id WHERE e.plan_id = ? AND e.slot < ? ORDER BY e.day, e.slot
                """, planId, perDay)) {
            out.computeIfAbsent(((Date) r.get("day")).toLocalDate(), k -> new ArrayList<>())
                    .add(new Entry(((Number) r.get("slot")).intValue(), (String) r.get("subject_id"), (String) r.get("subject")));
        }
        return out;
    }

    private Map<String, Object> planRow(String classSessionId, String termId) {
        var rows = jdbc.queryForList("SELECT id, school_id, status, cycle_weeks FROM weekly_plans WHERE class_session_id = ? AND term_id = ?",
                classSessionId, termId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String ensurePlan(String schoolId, String classSessionId, String termId) {
        var row = planRow(classSessionId, termId);
        if (row != null) return (String) row.get("id");
        String id = UUID.randomUUID().toString();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO weekly_plans (id, school_id, class_session_id, term_id, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'DRAFT', ?, ?)
                """, id, schoolId, classSessionId, termId, now, now);
        return id;
    }

    private void insertEntry(String planId, LocalDate day, int slot, String subjectId) {
        jdbc.update("INSERT INTO weekly_plan_entries (id, plan_id, day, slot, subject_id) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), planId, Date.valueOf(day), slot, subjectId);
    }

    private void touch(String planId) {
        jdbc.update("UPDATE weekly_plans SET updated_at = ? WHERE id = ?", Timestamp.from(Instant.now()), planId);
    }

    private static String weekday(LocalDate d) {
        return d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
    }

    static String label(LocalDate from, LocalDate to) {
        return (from.getMonth() == to.getMonth() ? String.valueOf(from.getDayOfMonth()) : from.format(DAY)) + " - "
                + to.format(DAY);
    }

    /** Today in the school's time zone (for callers without one). */
    public static LocalDate today() {
        return LocalDate.now(SchoolTimeZone.ZONE);
    }
}
