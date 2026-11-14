package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.usecase.LogActivityUseCase;
import com.moriba.skultem.domain.model.FeeDiscount.Kind;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.FeatureModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Skully's messages to parents on WhatsApp: a welcome with how to sign in (to every parent with a phone whose account
 * has never been used), a note when another child is linked to an existing parent, and reminders - exams coming up,
 * school events and holidays, and fees due. Skully
 * (the Worker) asks for what is due a few times a day ({@link #due}), sends each parent one message, and reports
 * back ({@link #result}). Each reminder goes out once, a parent gets at most one reminder message a day from a
 * school (except a day-before reminder, which can't wait until tomorrow), and nothing goes to a school without the
 * Skully module, a reminder kind the school switched off, or a parent who replied STOP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkullyReminderService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    // WhatsApp template values can't hold new lines and should stay short.
    private static final int MAX_TEXT = 900;
    // A Skully run takes seconds; a row still SENDING after this was never reported back (the Worker failed
    // between asking and reporting), so it is let go and tried again.
    private static final Duration STALE_SENDING = Duration.ofHours(1);
    // A STOP is matched on the last 8 digits (a Sierra Leone number without its country code or leading 0), so it
    // still counts if the school saved the number in another format. The Skully page counts opt-outs the same way.
    private static final int OPT_OUT_DIGITS = 8;
    // A child linked to an existing parent is announced if the link is this recent (so turning this on doesn't
    // announce every old link). A parent created within minutes of the link is new - the welcome covers them.
    private static final Duration LINK_WINDOW = Duration.ofDays(3);
    private static final String WELCOME_KEY = "welcome";

    /** What Skully can send, one WhatsApp template each. */
    public static final String REMINDER = "REMINDER", WELCOME = "WELCOME", LINKED = "LINKED", WEEKLY = "WEEKLY";
    public static final Set<String> ALL_KINDS = Set.of(REMINDER, WELCOME, LINKED, WEEKLY);
    // The kinds of item that make up the one reminder message a day.
    private static final List<String> REMINDER_ITEMS = List.of("EXAM", "EVENT", "FEE");

    private final JdbcTemplate jdbc;
    private final ModuleAccessService modules;
    private final FeeDiscountRepository discountRepo;
    private final PaymentRepository paymentRepo;
    private final LogActivityUseCase activity;

    @Value("${skully.default-country-code:232}")
    private String countryCode;

    @Value("${skully.currency-prefix:Le }")
    private String currency;

    @Value("${app.portal-url:https://{domain}.skultem.space}")
    private String portalUrl;

    public record Settings(boolean examReminders, boolean eventReminders, boolean feeReminders, boolean weeklySummary) {
    }

    public record SettingsView(boolean enabled, Settings settings, long sentThisMonth, long optedOut) {
    }

    /**
     * One WhatsApp message for one parent. kind picks the template (REMINDER, WELCOME, LINKED) and values fill its
     * {{1}}, {{2}}... in order. text is a short description (for a reminder, the reminder itself).
     */
    public record Message(String id, String kind, String phone, String parentName, String schoolName, String text,
            List<String> values) {
    }

    /** A parent who hasn't had the welcome yet, and why (the school's Skully page). */
    public record WelcomeWaiting(String parentId, String name, String phone, String children, String reason) {
    }

    public record WelcomeTracking(long sent, long waiting, List<WelcomeWaiting> parents) {
    }

    // A parent whose account has never been used, with what the welcome needs.
    private record Account(String parentId, String savedPhone, String phone, String name, String familyName,
            String email, String temporaryPassword, String schoolName, String loginUrl, List<String> children,
            boolean sending) {
    }

    /** urgent: about tomorrow, so it is sent even if the parent already had a message today. */
    private record Item(String schoolId, String parentId, String kind, String key, String text, boolean urgent) {
    }

    private record Recipient(String parentId, String phone, String name, String schoolName, String schoolDomain) {
    }

    // ---------------------------------------------------------------------------------------------------------
    // What is due

    public List<Message> due(int limit) {
        return due(limit, Set.of(REMINDER));
    }

    /** Messages Skully should send now, of the kinds it has a template for. Each counts as handed over until {@link #result}. */
    @Transactional
    public List<Message> due(int limit, Set<String> kinds) {
        LocalDate today = LocalDate.now(SchoolTimeZone.ZONE);
        List<Message> out = new ArrayList<>();
        int stale = jdbc.update("DELETE FROM skully_reminders WHERE status = 'SENDING' AND updated_at < ?",
                Timestamp.from(Instant.now().minus(STALE_SENDING)));
        if (stale > 0) log.warn("Skully reminders: {} never reported back, trying them again", stale);
        Set<String> optedOut = optedOut();

        for (String schoolId : schoolsWithSkully()) {
            if (out.size() >= limit) break;
            try {
                // Sign-in details first: a parent can't use anything else until they have them.
                if (kinds.contains(WELCOME)) welcomes(schoolId, optedOut, limit, out);
                if (kinds.contains(LINKED)) linked(schoolId, optedOut, limit, out);
                if (kinds.contains(REMINDER)) reminders(schoolId, today, optedOut, limit, out);
                if (kinds.contains(WEEKLY) && weeklyTime(ZonedDateTime.now(SchoolTimeZone.ZONE))
                        && settings(schoolId).weeklySummary()) weekly(schoolId, today, optedOut, limit, out);
            } catch (RuntimeException e) {
                log.error("Skully messages for school {} failed: {}", schoolId, e.getMessage(), e);
            }
        }
        return out;
    }

    private void welcomes(String schoolId, Set<String> optedOut, int limit, List<Message> out) {
        for (Account a : notWelcomed(schoolId)) {
            if (out.size() >= limit) return;
            if (a.sending() || a.phone() == null || a.temporaryPassword() == null
                    || optedOut.contains(optOutKey(a.phone()))) continue;
            String messageId = claim(schoolId, a.parentId(), a.phone(), WELCOME, List.of(WELCOME_KEY));
            if (messageId == null) continue;
            // Must match the approved skultem_create_new_parent template: 6 values, the temporary password last.
            String signInWith = a.email() != null ? a.email() : a.savedPhone();
            out.add(new Message(messageId, WELCOME, a.phone(), a.name(), a.schoolName(), "Welcome and sign-in details",
                    List.of(a.name(), a.schoolName(), names(a.children()), a.loginUrl(), signInWith,
                            a.temporaryPassword())));
        }
    }

    // Another child linked to a parent who already had their account (signed in, or had the welcome before the link).
    private void linked(String schoolId, Set<String> optedOut, int limit, List<Message> out) {
        var rows = jdbc.queryForList("""
                SELECT sp.parent_id, sp.student_id, st.given_names
                FROM student_parents sp
                JOIN parents p ON p.id = sp.parent_id AND p.status = 'ACTIVE'
                JOIN users u ON u.id = p.user_id
                JOIN students st ON st.id = sp.student_id AND st.status = 'ACTIVE'
                WHERE sp.school_id = ? AND sp.status = 'ACTIVE' AND sp.created_at >= ?
                  AND p.created_at < sp.created_at - interval '10 minutes'
                  AND (u.status = 'ACTIVE' OR EXISTS (SELECT 1 FROM skully_reminders r WHERE r.parent_id = p.id
                       AND r.item_key = ? AND r.status = 'SENT' AND r.created_at < sp.created_at))
                  AND NOT EXISTS (SELECT 1 FROM skully_reminders r WHERE r.parent_id = sp.parent_id
                       AND r.item_key = 'linked:' || sp.student_id)
                ORDER BY sp.created_at
                """, schoolId, Timestamp.from(Instant.now().minus(LINK_WINDOW)), WELCOME_KEY);
        Map<String, List<Map<String, Object>>> byParent = new LinkedHashMap<>();
        for (var r : rows) byParent.computeIfAbsent((String) r.get("parent_id"), k -> new ArrayList<>()).add(r);
        if (byParent.isEmpty()) return;

        Map<String, Recipient> recipients = recipients(schoolId, byParent.keySet());
        for (var entry : byParent.entrySet()) {
            if (out.size() >= limit) return;
            Recipient to = recipients.get(entry.getKey());
            if (to == null || to.phone() == null || optedOut.contains(optOutKey(to.phone()))) continue;
            List<String> keys = entry.getValue().stream().map(r -> "linked:" + r.get("student_id")).toList();
            String messageId = claim(schoolId, to.parentId(), to.phone(), LINKED, keys);
            if (messageId == null) continue;
            String children = names(entry.getValue().stream().map(r -> first(r.get("given_names"))).toList());
            out.add(new Message(messageId, LINKED, to.phone(), to.name(), to.schoolName(),
                    children + " linked to your account",
                    List.of(to.name(), to.schoolName(), children, loginUrl(to.schoolDomain()))));
        }
    }

    private void reminders(String schoolId, LocalDate today, Set<String> optedOut, int limit, List<Message> out) {
        Settings settings = settings(schoolId);
        List<Item> items = new ArrayList<>();
        if (settings.examReminders()) items.addAll(exams(schoolId, today));
        if (settings.eventReminders()) items.addAll(events(schoolId, today));
        if (settings.feeReminders()) items.addAll(fees(schoolId, today));
        if (items.isEmpty()) return;

        // Already sent, or the parent already had today's reminder message from this school (unless it is about
        // tomorrow). Welcome and linked messages don't count: they are messages of their own.
        Set<String> sentKeys = new HashSet<>(jdbc.queryForList(
                "SELECT parent_id || '|' || item_key FROM skully_reminders WHERE school_id = ?", String.class, schoolId));
        Set<String> doneToday = new HashSet<>(jdbc.queryForList("""
                SELECT DISTINCT parent_id FROM skully_reminders
                WHERE school_id = ? AND created_at >= ? AND kind IN ('EXAM', 'EVENT', 'FEE')
                """, String.class, schoolId, Timestamp.from(today.atStartOfDay(SchoolTimeZone.ZONE).toInstant())));

        Map<String, List<Item>> byParent = new LinkedHashMap<>();
        for (Item item : items) {
            if (sentKeys.contains(item.parentId() + "|" + item.key())) continue;
            if (!item.urgent() && doneToday.contains(item.parentId())) continue;
            byParent.computeIfAbsent(item.parentId(), k -> new ArrayList<>()).add(item);
        }
        if (byParent.isEmpty()) return;

        Map<String, Recipient> recipients = recipients(schoolId, byParent.keySet());
        for (var entry : byParent.entrySet()) {
            if (out.size() >= limit) return;
            Recipient to = recipients.get(entry.getKey());
            if (to == null || to.phone() == null || optedOut.contains(optOutKey(to.phone()))) continue;
            // Several items about the same thing (two children, same event) are said once.
            List<Item> parts = new ArrayList<>();
            Set<String> texts = new HashSet<>();
            for (Item i : entry.getValue()) if (texts.add(i.text())) parts.add(i);

            String text = join(parts.stream().map(Item::text).toList());
            String messageId = UUID.randomUUID().toString();
            Timestamp now = Timestamp.from(Instant.now());
            for (Item i : entry.getValue()) {
                jdbc.update("""
                        INSERT INTO skully_reminders (id, message_id, school_id, parent_id, phone, kind, item_key,
                            status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'SENDING', ?, ?) ON CONFLICT (parent_id, item_key) DO NOTHING
                        """, UUID.randomUUID().toString(), messageId, schoolId, i.parentId(), to.phone(), i.kind(),
                        i.key(), now, now);
            }
            out.add(new Message(messageId, REMINDER, to.phone(), to.name(), to.schoolName(), text,
                    List.of(to.name(), to.schoolName(), text)));
        }
    }

    // Records a message's items as handed to Skully. null when they were already claimed (another run has them).
    private String claim(String schoolId, String parentId, String phone, String kind, List<String> keys) {
        String messageId = UUID.randomUUID().toString();
        Timestamp now = Timestamp.from(Instant.now());
        int claimed = 0;
        for (String key : keys) {
            claimed += jdbc.update("""
                    INSERT INTO skully_reminders (id, message_id, school_id, parent_id, phone, kind, item_key,
                        status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'SENDING', ?, ?) ON CONFLICT (parent_id, item_key) DO NOTHING
                    """, UUID.randomUUID().toString(), messageId, schoolId, parentId, phone, kind, key, now, now);
        }
        return claimed > 0 ? messageId : null;
    }

    private Set<String> optedOut() {
        Set<String> out = new HashSet<>();
        for (String phone : jdbc.queryForList("SELECT phone FROM skully_opt_outs", String.class)) out.add(optOutKey(phone));
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The weekly academic summary: on Friday afternoon (Saturday as a catch-up), one message per child with their
    // week - marks the school approved this week, attendance, and behaviour notes as the teacher's remark. A child
    // with nothing recorded this week gets no message. Fills the approved weekly_academic_summary template.

    static boolean weeklyTime(ZonedDateTime now) {
        return (now.getDayOfWeek() == DayOfWeek.FRIDAY && now.getHour() >= 14) || now.getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    private static final DateTimeFormatter WEEK_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final int MAX_REMARK = 300;

    /** "10 - 14 Nov 2026": Monday to Friday of this week. */
    static String weekLabel(LocalDate monday) {
        LocalDate friday = monday.plusDays(4);
        String from = monday.getMonth() == friday.getMonth() ? String.valueOf(monday.getDayOfMonth()) : monday.format(WEEK_DAY);
        return from + " - " + friday.format(WEEK_DAY) + " " + friday.getYear();
    }

    private static final class Week {
        long days, present;
        final List<Integer> scores = new ArrayList<>();
        final Set<String> subjects = new java.util.TreeSet<>();
        String best;
        int bestScore = -1;
        final List<String> remarks = new ArrayList<>();

        boolean empty() {
            return days == 0 && scores.isEmpty() && remarks.isEmpty();
        }

        void mark(String subject, int score) {
            scores.add(score);
            subjects.add(subject);
            if (score > bestScore) {
                bestScore = score;
                best = score + "% (" + subject + ")";
            }
        }

        /** {{6}} to {{11}} of the template. */
        List<String> values() {
            boolean marked = !scores.isEmpty();
            String average = marked
                    ? Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0)) + "%" : "No marks this week";
            String remark = remarks.isEmpty() ? "Normal this week" : String.join("; ", remarks);
            if (remark.length() > MAX_REMARK) remark = remark.substring(0, MAX_REMARK - 3).trim() + "...";
            return List.of(
                    marked ? subjects.size() + " (" + String.join(", ", subjects) + ")" : "None this week",
                    average,
                    marked ? best : "-",
                    days == 0 ? "Not recorded" : present + " of " + days,
                    days == 0 ? "Not recorded" : String.valueOf(days - present),
                    remark);
        }
    }

    private void weekly(String schoolId, LocalDate today, Set<String> optedOut, int limit, List<Message> out) {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        Timestamp weekStart = Timestamp.from(monday.atStartOfDay(SchoolTimeZone.ZONE).toInstant());
        Map<String, Week> weeks = new LinkedHashMap<>();

        for (var r : jdbc.queryForList("""
                SELECT e.student_id,
                       COUNT(*) FILTER (WHERE NOT a.holiday) AS days,
                       COUNT(*) FILTER (WHERE a.present AND NOT a.holiday) AS present
                FROM attendance a JOIN enrollments e ON e.id = a.enrollment_id AND e.status = 'ACTIVE'
                WHERE a.school_id = ? AND a.date BETWEEN ? AND ?
                GROUP BY e.student_id
                """, schoolId, Date.valueOf(monday), Date.valueOf(today))) {
            Week w = weeks.computeIfAbsent((String) r.get("student_id"), k -> new Week());
            w.days = ((Number) r.get("days")).longValue();
            w.present = ((Number) r.get("present")).longValue();
        }

        // Marks once the school has approved them (what report cards use), not drafts a teacher may still change.
        for (var r : jdbc.queryForList("""
                SELECT e.student_id, s.name AS subject, sc.score
                FROM assessment_scores sc
                JOIN class_subject_assessment_life_cycle lc ON lc.id = sc.class_subject_assessment_life_cycle_id
                    AND lc.status IN ('APPROVED', 'COMPLETED', 'LOCKED') AND lc.updated_at >= ?
                JOIN teacher_subjects ts ON ts.id = lc.teacher_subject_id
                JOIN subjects s ON s.id = ts.subject_id
                JOIN student_assessments sa ON sa.id = sc.student_assessment_id
                JOIN enrollments e ON e.id = sa.enrollment_id AND e.status = 'ACTIVE'
                WHERE sc.school_id = ?
                """, weekStart, schoolId)) {
            weeks.computeIfAbsent((String) r.get("student_id"), k -> new Week())
                    .mark((String) r.get("subject"), ((Number) r.get("score")).intValue());
        }

        // Behaviour notes are the teacher's remark: positive ones first, said as written.
        for (var r : jdbc.queryForList("""
                SELECT e.student_id, b.kind, b.note
                FROM behaviours b JOIN enrollments e ON e.id = b.enrollment_id AND e.status = 'ACTIVE'
                WHERE b.school_id = ? AND b.created_at >= ?
                ORDER BY CASE b.kind WHEN 'POSITIVE' THEN 0 WHEN 'NEUTRAL' THEN 1 ELSE 2 END, b.created_at
                """, schoolId, weekStart)) {
            String note = Optional.ofNullable((String) r.get("note")).map(n -> n.replaceAll("\\s+", " ").trim()).orElse("");
            if (note.isEmpty()) continue;
            String kind = (String) r.get("kind");
            weeks.computeIfAbsent((String) r.get("student_id"), k -> new Week()).remarks
                    .add("NEGATIVE".equals(kind) ? "To talk about: " + note : note);
        }

        weeks.values().removeIf(Week::empty);
        if (weeks.isEmpty()) return;

        // The children, their class and their parent.
        String in = String.join(",", weeks.keySet().stream().map(k -> "?").toList());
        List<Object> args = new ArrayList<>(List.of(schoolId));
        args.addAll(weeks.keySet());
        var students = jdbc.queryForList("""
                SELECT st.id, st.parent_id, st.given_names, st.family_name, c.name AS class_name, sec.name AS section
                FROM students st
                JOIN enrollments e ON e.student_id = st.id AND e.status = 'ACTIVE'
                JOIN classes c ON c.id = e.class_id
                LEFT JOIN sections sec ON sec.id = e.section_id
                WHERE st.school_id = ? AND st.status = 'ACTIVE' AND st.parent_id IS NOT NULL AND st.id IN (""" + in + ")",
                args.toArray());
        Map<String, Recipient> recipients = recipients(schoolId,
                students.stream().map(s -> (String) s.get("parent_id")).collect(java.util.stream.Collectors.toSet()));
        String week = weekLabel(monday);

        for (var s : students) {
            if (out.size() >= limit) return;
            Recipient to = recipients.get((String) s.get("parent_id"));
            if (to == null || to.phone() == null || optedOut.contains(optOutKey(to.phone()))) continue;
            String studentId = (String) s.get("id");
            String messageId = claim(schoolId, to.parentId(), to.phone(), WEEKLY, List.of("weekly:" + monday + ":" + studentId));
            if (messageId == null) continue;
            String child = (Optional.ofNullable((String) s.get("given_names")).orElse("") + " "
                    + Optional.ofNullable((String) s.get("family_name")).orElse("")).trim();
            String section = (String) s.get("section");
            String clazz = s.get("class_name") + (section == null || section.isBlank() ? "" : " " + section);
            List<String> values = new ArrayList<>(List.of(to.name(), to.schoolName(), child, week, clazz));
            values.addAll(weeks.get(studentId).values());
            values.add(portal(to.schoolDomain()));
            out.add(new Message(messageId, WEEKLY, to.phone(), to.name(), to.schoolName(), child + "'s week", values));
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The welcome is decided from the account: every parent with a phone who has never signed in gets it once -
    // parents added before Skully included - and the school can see who is still waiting and why.

    private List<Account> notWelcomed(String schoolId) {
        return jdbc.query("""
                SELECT p.id, p.phone, u.given_name, u.family_name, u.email, u.hint, s.name AS school_name, s.domain,
                       ARRAY(SELECT st.given_names FROM students st WHERE st.parent_id = p.id AND st.status = 'ACTIVE'
                             ORDER BY st.given_names) AS children,
                       EXISTS (SELECT 1 FROM skully_reminders r WHERE r.parent_id = p.id AND r.item_key = ?) AS sending
                FROM parents p JOIN users u ON u.id = p.user_id JOIN schools s ON s.id = p.school_id
                WHERE p.school_id = ? AND p.status = 'ACTIVE' AND u.status = 'RESET_PASSWORD'
                  AND NOT EXISTS (SELECT 1 FROM skully_reminders r WHERE r.parent_id = p.id AND r.item_key = ?
                                  AND r.status = 'SENT')
                ORDER BY p.created_at
                """, (rs, n) -> {
            var childArray = rs.getArray("children");
            List<String> children = new ArrayList<>();
            if (childArray != null) for (Object c : (Object[]) childArray.getArray()) children.add(first(c));
            String hint = rs.getString("hint");
            String email = rs.getString("email");
            return new Account(rs.getString("id"), rs.getString("phone"), whatsappNumber(rs.getString("phone")),
                    Optional.ofNullable(rs.getString("given_name")).filter(v -> !v.isBlank()).orElse("Parent"),
                    rs.getString("family_name"), email == null || email.isBlank() ? null : email.trim(),
                    hint == null || hint.isBlank() ? null : hint, rs.getString("school_name"),
                    loginUrl(rs.getString("domain")), children, rs.getBoolean("sending"));
        }, WELCOME_KEY, schoolId, WELCOME_KEY);
    }

    /** Who at this school hasn't had the welcome, and what is holding each one up. */
    public WelcomeTracking welcomeTracking(String schoolId) {
        boolean on = modules.isEnabled(schoolId, FeatureModule.WHATSAPP_ASSISTANT);
        Set<String> optedOut = optedOut();
        List<WelcomeWaiting> waiting = new ArrayList<>();
        for (Account a : notWelcomed(schoolId)) {
            String reason = !on ? "Skully is off for this school"
                    : a.phone() == null ? "The phone number can't be used on WhatsApp - correct it on the guardian"
                    : optedOut.contains(optOutKey(a.phone())) ? "Replied STOP to Skully"
                    : a.temporaryPassword() == null ? "No temporary password - issue a new one from their account"
                    : a.sending() ? "Sending now"
                    : "Waiting - goes out on Skully's next run (hourly, 08:00-16:00)";
            String name = (a.name() + " " + Optional.ofNullable(a.familyName()).orElse("")).trim();
            waiting.add(new WelcomeWaiting(a.parentId(), name, a.savedPhone(), names(a.children()), reason));
        }
        Long sent = jdbc.queryForObject(
                "SELECT COUNT(*) FROM skully_reminders WHERE school_id = ? AND item_key = ? AND status = 'SENT'",
                Long.class, schoolId, WELCOME_KEY);
        return new WelcomeTracking(sent == null ? 0 : sent, waiting.size(), waiting.stream().limit(500).toList());
    }

    /** What Skully could send: sent messages are kept, failed ones are forgotten so they are tried again later. */
    @Transactional
    public void result(Collection<String> sent, Collection<String> failed) {
        Timestamp now = Timestamp.from(Instant.now());
        for (String id : sent == null ? List.<String>of() : sent) {
            jdbc.update("UPDATE skully_reminders SET status = 'SENT', updated_at = ? WHERE message_id = ?", now, id);
            // A temporary password went out: the school's activity log says so, as when Skully hands it over in a chat.
            for (var r : jdbc.queryForList("""
                    SELECT r.school_id, u.id AS user_id, u.given_name, u.family_name FROM skully_reminders r
                    JOIN parents p ON p.id = r.parent_id JOIN users u ON u.id = p.user_id
                    WHERE r.message_id = ? AND r.item_key = ?
                    """, id, WELCOME_KEY)) {
                activity.log((String) r.get("school_id"), ActivityType.USER,
                        "Welcome and sign-in details sent on WhatsApp (Skully)",
                        (Optional.ofNullable((String) r.get("given_name")).orElse("") + " "
                                + Optional.ofNullable((String) r.get("family_name")).orElse("")).trim(),
                        null, (String) r.get("user_id"));
            }
        }
        for (String id : failed == null ? List.<String>of() : failed) {
            jdbc.update("DELETE FROM skully_reminders WHERE message_id = ? AND status = 'SENDING'", id);
        }
    }

    /** A parent replied STOP (out = true) or START. */
    @Transactional
    public void optOut(String phone, boolean out) {
        String number = whatsappNumber(phone);
        if (number == null) return;
        if (out) {
            jdbc.update("INSERT INTO skully_opt_outs (phone, created_at) VALUES (?, ?) ON CONFLICT (phone) DO NOTHING",
                    number, Timestamp.from(Instant.now()));
        } else {
            jdbc.update("DELETE FROM skully_opt_outs WHERE phone = ?", number);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The school's settings (Skully page)

    public Settings settings(String schoolId) {
        var rows = jdbc.query("""
                SELECT exam_reminders, event_reminders, fee_reminders, weekly_summary FROM skully_settings WHERE school_id = ?
                """, (rs, n) -> new Settings(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3), rs.getBoolean(4)), schoolId);
        return rows.isEmpty() ? new Settings(true, true, true, true) : rows.get(0);
    }

    @Transactional
    public SettingsView saveSettings(String schoolId, Settings s) {
        jdbc.update("""
                INSERT INTO skully_settings (school_id, exam_reminders, event_reminders, fee_reminders, weekly_summary,
                    updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (school_id) DO UPDATE SET exam_reminders = EXCLUDED.exam_reminders,
                    event_reminders = EXCLUDED.event_reminders, fee_reminders = EXCLUDED.fee_reminders,
                    weekly_summary = EXCLUDED.weekly_summary, updated_at = EXCLUDED.updated_at
                """, schoolId, s.examReminders(), s.eventReminders(), s.feeReminders(), s.weeklySummary(),
                Timestamp.from(Instant.now()));
        return view(schoolId);
    }

    public SettingsView view(String schoolId) {
        Instant monthStart = LocalDate.now(SchoolTimeZone.ZONE).withDayOfMonth(1).atStartOfDay(SchoolTimeZone.ZONE).toInstant();
        Long sent = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT message_id) FROM skully_reminders WHERE school_id = ? AND status = 'SENT' AND created_at >= ?",
                Long.class, schoolId, Timestamp.from(monthStart));
        Long optedOut = jdbc.queryForObject("""
                SELECT COUNT(*) FROM skully_opt_outs o WHERE EXISTS (SELECT 1 FROM parents p WHERE p.school_id = ?
                    AND right(regexp_replace(p.phone, '[^0-9]', '', 'g'), ?) = right(o.phone, ?))
                """, Long.class, schoolId, OPT_OUT_DIGITS, OPT_OUT_DIGITS);
        return new SettingsView(modules.isEnabled(schoolId, FeatureModule.WHATSAPP_ASSISTANT), settings(schoolId),
                sent == null ? 0 : sent, optedOut == null ? 0 : optedOut);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The reminders

    // A child's exams: once in the week before the first paper, and again the day before.
    List<Item> exams(String schoolId, LocalDate today) {
        var rows = jdbc.queryForList("""
                SELECT st.id AS student_id, st.parent_id, st.given_names, t.id AS timetable_id, t.title,
                       e.class_id, MIN(p.exam_date) AS first_day
                FROM exam_timetables t
                JOIN terms tm ON tm.id = t.term_id
                JOIN academic_years ay ON ay.id = tm.academic_year_id AND ay.active = TRUE
                JOIN exam_papers p ON p.timetable_id = t.id
                JOIN exam_paper_classes pc ON pc.paper_id = p.id
                JOIN enrollments e ON e.class_id = pc.class_id AND e.academic_year_id = ay.id
                    AND e.school_id = t.school_id AND e.status = 'ACTIVE'
                JOIN students st ON st.id = e.student_id AND st.status = 'ACTIVE' AND st.parent_id IS NOT NULL
                WHERE t.school_id = ? AND t.status = 'PUBLISHED'
                GROUP BY st.id, st.parent_id, st.given_names, t.id, t.title, e.class_id
                HAVING MIN(p.exam_date) BETWEEN ? AND ?
                """, schoolId, Date.valueOf(today.plusDays(1)), Date.valueOf(today.plusDays(7)));
        List<Item> out = new ArrayList<>();
        for (var r : rows) {
            LocalDate first = ((Date) r.get("first_day")).toLocalDate();
            long days = ChronoUnit.DAYS.between(today, first);
            String when = days == 1 ? "tomorrow (" + first.format(DAY) + ")" : "on " + first.format(DAY);
            List<String> subjects = jdbc.queryForList("""
                    SELECT DISTINCT s.name FROM exam_papers p
                    JOIN exam_paper_classes pc ON pc.paper_id = p.id
                    JOIN subjects s ON s.id = p.subject_id
                    WHERE p.timetable_id = ? AND pc.class_id = ? AND p.exam_date = ?
                    """, String.class, r.get("timetable_id"), r.get("class_id"), Date.valueOf(first));
            String title = Optional.ofNullable((String) r.get("title")).filter(s -> !s.isBlank()).orElse("exams");
            String text = first(r.get("given_names")) + "'s " + title + " start " + when
                    + (subjects.isEmpty() ? "" : " with " + String.join(", ", subjects.stream().limit(3).toList()))
                    + " - please help them prepare";
            String key = "exam:" + r.get("timetable_id") + ":" + r.get("student_id") + ":" + (days == 1 ? "tomorrow" : "week");
            out.add(new Item(schoolId, (String) r.get("parent_id"), "EXAM", key, text, days == 1));
        }
        return out;
    }

    // Tomorrow's whole-school events and holidays, to every parent with a child at the school.
    List<Item> events(String schoolId, LocalDate today) {
        LocalDate tomorrow = today.plusDays(1);
        List<String[]> happenings = new ArrayList<>();
        for (var r : jdbc.queryForList("""
                SELECT id, title, location FROM calendar_events
                WHERE school_id = ? AND management_section_id IS NULL
                  AND (start_date AT TIME ZONE 'Africa/Freetown')::date = ?
                """, schoolId, Date.valueOf(tomorrow))) {
            String where = r.get("location") == null || ((String) r.get("location")).isBlank() ? ""
                    : " at " + r.get("location");
            happenings.add(new String[] { "event:" + r.get("id"), r.get("title") + " is tomorrow (" + tomorrow.format(DAY) + ")" + where });
        }
        for (var r : jdbc.queryForList("SELECT id, name FROM holidays WHERE school_id = ? AND date = ?",
                schoolId, Date.valueOf(tomorrow))) {
            happenings.add(new String[] { "holiday:" + r.get("id") + ":" + tomorrow,
                    "School is closed tomorrow (" + tomorrow.format(DAY) + ") for " + r.get("name") });
        }
        if (happenings.isEmpty()) return List.of();

        List<String> parents = jdbc.queryForList("""
                SELECT DISTINCT parent_id FROM students WHERE school_id = ? AND status = 'ACTIVE' AND parent_id IS NOT NULL
                """, String.class, schoolId);
        List<Item> out = new ArrayList<>();
        for (String parent : parents) {
            for (String[] h : happenings) out.add(new Item(schoolId, parent, "EVENT", h[0], h[1], true));
        }
        return out;
    }

    // Fees: in the 3 days before the due date, and once in the week after it if still unpaid.
    List<Item> fees(String schoolId, LocalDate today) {
        var rows = jdbc.queryForList("""
                SELECT DISTINCT st.id AS student_id, st.parent_id, st.given_names, f.id AS fee_id, f.amount, f.due_date,
                       COALESCE(c.name, f.description, 'school fees') AS item
                FROM student_fees sf
                JOIN fee_structures f ON f.id = sf.fee_id
                LEFT JOIN fee_categories c ON c.id = f.category_id
                JOIN students st ON st.id = sf.student_id AND st.status = 'ACTIVE' AND st.parent_id IS NOT NULL
                WHERE sf.school_id = ? AND f.due_date BETWEEN ? AND ?
                """, schoolId, Date.valueOf(today.minusDays(7)), Date.valueOf(today.plusDays(3)));
        List<Item> out = new ArrayList<>();
        for (var r : rows) {
            String studentId = (String) r.get("student_id");
            String feeId = (String) r.get("fee_id");
            BigDecimal amount = (BigDecimal) r.get("amount");
            BigDecimal owed = outstanding(schoolId, studentId, feeId, amount);
            if (owed.signum() <= 0) continue;
            LocalDate due = ((Date) r.get("due_date")).toLocalDate();
            boolean overdue = due.isBefore(today);
            String text = money(owed) + " for " + first(r.get("given_names")) + "'s " + r.get("item")
                    + (overdue ? " was due on " + due.format(DAY) + " and is still unpaid"
                            : due.equals(today) ? " is due today" : " is due on " + due.format(DAY));
            String key = "fee:" + feeId + ":" + studentId + ":" + (overdue ? "overdue" : "before");
            out.add(new Item(schoolId, (String) r.get("parent_id"), "FEE", key, text, false));
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------

    public List<String> schoolsWithSkully() {
        return jdbc.queryForList("""
                SELECT sm.school_id FROM school_modules sm JOIN schools s ON s.id = sm.school_id
                WHERE sm.module_key = ? AND sm.enabled = TRUE ORDER BY sm.school_id
                """, String.class, FeatureModule.WHATSAPP_ASSISTANT.name());
    }

    private Map<String, Recipient> recipients(String schoolId, Collection<String> parentIds) {
        Map<String, Recipient> out = new LinkedHashMap<>();
        if (parentIds.isEmpty()) return out;
        String in = String.join(",", parentIds.stream().map(p -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        args.addAll(parentIds);
        for (var r : jdbc.queryForList("""
                SELECT p.id, p.phone, u.given_name, s.name AS school_name, s.domain
                FROM parents p JOIN schools s ON s.id = p.school_id LEFT JOIN users u ON u.id = p.user_id
                WHERE p.school_id = ? AND p.id IN (""" + in + ")", args.toArray())) {
            out.put((String) r.get("id"), new Recipient((String) r.get("id"), whatsappNumber((String) r.get("phone")),
                    Optional.ofNullable((String) r.get("given_name")).filter(s -> !s.isBlank()).orElse("Parent"),
                    (String) r.get("school_name"), (String) r.get("domain")));
        }
        return out;
    }

    // Same rule as payments: the fee, less discounts, less what was paid.
    private BigDecimal outstanding(String schoolId, String studentId, String feeId, BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        BigDecimal discount = discountRepo.findBySchoolAndStudentIdAndFeeId(schoolId, studentId, feeId).stream()
                .map(d -> d.getKind() == Kind.PERCENTAGE
                        ? amount.multiply(d.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        : d.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = Optional.ofNullable(paymentRepo.sumPaymentsByStudentAndFee(studentId, feeId)).orElse(BigDecimal.ZERO);
        return amount.subtract(discount).subtract(paid).max(BigDecimal.ZERO);
    }

    /** A saved phone as a WhatsApp number: digits with the country code (077 123 456 -> 23277123456). */
    String whatsappNumber(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim();
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return null;
        String code = countryCode == null ? "" : countryCode.replaceAll("[^0-9]", "");
        if (trimmed.startsWith("+")) return digits;
        if (digits.startsWith("00")) return digits.substring(2);
        if (!code.isEmpty() && digits.startsWith(code) && digits.length() > code.length() + 6) return digits;
        if (digits.startsWith("0")) return code + digits.substring(1);
        return code + digits;
    }

    private String portal(String domain) {
        return portalUrl.replace("{domain}", domain == null ? "" : domain).replaceAll("/+$", "");
    }

    private String loginUrl(String domain) {
        return portal(domain) + "/login";
    }

    /** Fatu / Fatu and Musa / Fatu, Musa and Ali; "your child" when there are none. */
    static String names(List<String> names) {
        if (names.isEmpty()) return "your child";
        if (names.size() == 1) return names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    static String optOutKey(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        return digits.length() <= OPT_OUT_DIGITS ? digits : digits.substring(digits.length() - OPT_OUT_DIGITS);
    }

    static String join(List<String> parts) {
        StringBuilder text = new StringBuilder();
        int used = 0;
        for (String p : parts) {
            String clean = p.replaceAll("\\s+", " ").trim();
            if (text.length() + clean.length() + 2 > MAX_TEXT) break;
            if (!text.isEmpty()) text.append("; ");
            text.append(clean);
            used++;
        }
        if (used < parts.size()) text.append("; and ").append(parts.size() - used).append(" more - reply to ask");
        return text.toString();
    }

    private String money(BigDecimal value) {
        return currency + new DecimalFormat("#,##0.##").format(value);
    }

    private static String first(Object givenNames) {
        String s = givenNames == null ? "" : givenNames.toString().trim();
        int space = s.indexOf(' ');
        return space > 0 ? s.substring(0, space) : (s.isEmpty() ? "Your child" : s);
    }
}
