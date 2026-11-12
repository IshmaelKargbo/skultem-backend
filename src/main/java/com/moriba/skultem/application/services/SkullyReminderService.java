package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
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

import com.moriba.skultem.domain.model.FeeDiscount.Kind;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.FeatureModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Skully's reminders to parents on WhatsApp: exams coming up, school events and holidays, and fees due. Skully
 * (the Worker) asks for what is due a few times a day ({@link #due}), sends each parent one message, and reports
 * back ({@link #result}). Each reminder goes out once, a parent gets at most one reminder message a day from a
 * school, and nothing goes to a school without the Skully module, a reminder kind the school switched off, or a
 * parent who replied STOP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkullyReminderService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    // WhatsApp template values can't hold new lines and should stay short.
    private static final int MAX_TEXT = 900;

    private final JdbcTemplate jdbc;
    private final ModuleAccessService modules;
    private final FeeDiscountRepository discountRepo;
    private final PaymentRepository paymentRepo;

    @Value("${skully.default-country-code:232}")
    private String countryCode;

    @Value("${skully.currency-prefix:Le }")
    private String currency;

    public record Settings(boolean examReminders, boolean eventReminders, boolean feeReminders) {
    }

    public record SettingsView(boolean enabled, Settings settings, long sentThisMonth, long optedOut) {
    }

    /** One WhatsApp message for one parent. text is a single line (a template value). */
    public record Message(String id, String phone, String parentName, String schoolName, String text) {
    }

    private record Item(String schoolId, String parentId, String kind, String key, String text) {
    }

    private record Recipient(String parentId, String phone, String name, String schoolName) {
    }

    // ---------------------------------------------------------------------------------------------------------
    // What is due

    @Transactional
    public List<Message> due(int limit) {
        LocalDate today = LocalDate.now(SchoolTimeZone.ZONE);
        List<Message> out = new ArrayList<>();
        Set<String> optedOut = new HashSet<>(jdbc.queryForList("SELECT phone FROM skully_opt_outs", String.class));

        for (String schoolId : schoolsWithSkully()) {
            if (out.size() >= limit) break;
            Settings settings = settings(schoolId);
            List<Item> items = new ArrayList<>();
            try {
                if (settings.examReminders()) items.addAll(exams(schoolId, today));
                if (settings.eventReminders()) items.addAll(events(schoolId, today));
                if (settings.feeReminders()) items.addAll(fees(schoolId, today));
            } catch (RuntimeException e) {
                log.error("Skully reminders for school {} failed: {}", schoolId, e.getMessage(), e);
                continue;
            }
            if (items.isEmpty()) continue;

            // Already sent, or the parent already had today's message from this school.
            Set<String> sentKeys = new HashSet<>(jdbc.queryForList(
                    "SELECT parent_id || '|' || item_key FROM skully_reminders WHERE school_id = ?", String.class, schoolId));
            Set<String> doneToday = new HashSet<>(jdbc.queryForList(
                    "SELECT DISTINCT parent_id FROM skully_reminders WHERE school_id = ? AND created_at >= ?",
                    String.class, schoolId, Timestamp.from(today.atStartOfDay(SchoolTimeZone.ZONE).toInstant())));

            Map<String, List<Item>> byParent = new LinkedHashMap<>();
            for (Item item : items) {
                if (sentKeys.contains(item.parentId() + "|" + item.key()) || doneToday.contains(item.parentId())) continue;
                byParent.computeIfAbsent(item.parentId(), k -> new ArrayList<>()).add(item);
            }
            if (byParent.isEmpty()) continue;

            Map<String, Recipient> recipients = recipients(schoolId, byParent.keySet());
            for (var entry : byParent.entrySet()) {
                if (out.size() >= limit) break;
                Recipient to = recipients.get(entry.getKey());
                if (to == null || to.phone() == null || optedOut.contains(to.phone())) continue;
                // Several items about the same thing (two children, same event) are said once.
                List<Item> parts = new ArrayList<>();
                Set<String> texts = new HashSet<>();
                for (Item i : entry.getValue()) if (texts.add(i.text())) parts.add(i);

                String text = join(parts.stream().map(Item::text).toList());
                String messageId = UUID.randomUUID().toString();
                Instant now = Instant.now();
                for (Item i : entry.getValue()) {
                    jdbc.update("""
                            INSERT INTO skully_reminders (id, message_id, school_id, parent_id, phone, kind, item_key,
                                status, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'SENDING', ?, ?) ON CONFLICT (parent_id, item_key) DO NOTHING
                            """, UUID.randomUUID().toString(), messageId, schoolId, i.parentId(), to.phone(), i.kind(),
                            i.key(), Timestamp.from(now), Timestamp.from(now));
                }
                out.add(new Message(messageId, to.phone(), to.name(), to.schoolName(), text));
            }
        }
        return out;
    }

    /** What Skully could send: sent messages are kept, failed ones are forgotten so they are tried again later. */
    @Transactional
    public void result(Collection<String> sent, Collection<String> failed) {
        Timestamp now = Timestamp.from(Instant.now());
        for (String id : sent == null ? List.<String>of() : sent) {
            jdbc.update("UPDATE skully_reminders SET status = 'SENT', updated_at = ? WHERE message_id = ?", now, id);
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
        var rows = jdbc.query("SELECT exam_reminders, event_reminders, fee_reminders FROM skully_settings WHERE school_id = ?",
                (rs, n) -> new Settings(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3)), schoolId);
        return rows.isEmpty() ? new Settings(true, true, true) : rows.get(0);
    }

    @Transactional
    public SettingsView saveSettings(String schoolId, Settings s) {
        jdbc.update("""
                INSERT INTO skully_settings (school_id, exam_reminders, event_reminders, fee_reminders, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (school_id) DO UPDATE SET exam_reminders = EXCLUDED.exam_reminders,
                    event_reminders = EXCLUDED.event_reminders, fee_reminders = EXCLUDED.fee_reminders,
                    updated_at = EXCLUDED.updated_at
                """, schoolId, s.examReminders(), s.eventReminders(), s.feeReminders(), Timestamp.from(Instant.now()));
        return view(schoolId);
    }

    public SettingsView view(String schoolId) {
        Instant monthStart = LocalDate.now(SchoolTimeZone.ZONE).withDayOfMonth(1).atStartOfDay(SchoolTimeZone.ZONE).toInstant();
        Long sent = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT message_id) FROM skully_reminders WHERE school_id = ? AND status = 'SENT' AND created_at >= ?",
                Long.class, schoolId, Timestamp.from(monthStart));
        Long optedOut = jdbc.queryForObject("""
                SELECT COUNT(*) FROM skully_opt_outs o WHERE EXISTS (SELECT 1 FROM parents p WHERE p.school_id = ?
                    AND right(regexp_replace(p.phone, '[^0-9]', '', 'g'), 8) = right(o.phone, 8))
                """, Long.class, schoolId);
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
            out.add(new Item(schoolId, (String) r.get("parent_id"), "EXAM", key, text));
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
            for (String[] h : happenings) out.add(new Item(schoolId, parent, "EVENT", h[0], h[1]));
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
            out.add(new Item(schoolId, (String) r.get("parent_id"), "FEE", key, text));
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
                SELECT p.id, p.phone, u.given_name, s.name AS school_name
                FROM parents p JOIN schools s ON s.id = p.school_id LEFT JOIN users u ON u.id = p.user_id
                WHERE p.school_id = ? AND p.id IN (""" + in + ")", args.toArray())) {
            out.put((String) r.get("id"), new Recipient((String) r.get("id"), whatsappNumber((String) r.get("phone")),
                    Optional.ofNullable((String) r.get("given_name")).filter(s -> !s.isBlank()).orElse("Parent"),
                    (String) r.get("school_name")));
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
