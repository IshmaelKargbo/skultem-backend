package com.moriba.skultem.application.services;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.GenerateStudentsRequiringAttentionUseCase;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.security.AuthUser;

import lombok.RequiredArgsConstructor;

/**
 * School staff on Skully. Skully recognises a staff member by the WhatsApp number on their account (users.phone,
 * digits with the country code) or, for a teacher, the phone on their teacher profile - always the whole number.
 * They can ask about their school, only what their role can see in Skultem itself:
 * <ul>
 * <li>fees: owner, proprietor, principal, super admin, accountant (as the fee reports)</li>
 * <li>attendance and academics (students and classes needing attention): owner, proprietor, principal, super
 * admin, admin, exam officer (as the attention reports)</li>
 * <li>myClasses: a teacher's own classes only - the class they are class teacher of (attendance, students needing
 * attention) and the subjects they teach (class average, students below 50%)</li>
 * </ul>
 * A section-limited admin or principal only sees their own sections: the reports run as that user. Someone who is
 * also a parent keeps their parent side separately (SkullyParentService, matched on the guardian's phone). Read-only,
 * and only at schools with Skully switched on.
 */
@Service
@RequiredArgsConstructor
public class SkullyStaffService {

    // Most senior first: the role a school overview runs as (and so its section scope) is the first one they have.
    static final List<Role> STAFF_ROLES = List.of(Role.OWNER, Role.PROPRIETOR, Role.SUPER_ADMIN, Role.PRINCIPAL,
            Role.ADMIN, Role.EXAM_OFFICER, Role.ACCOUNTANT, Role.TEACHER);
    static final Set<Role> FEES = EnumSet.of(Role.OWNER, Role.PROPRIETOR, Role.PRINCIPAL, Role.SUPER_ADMIN, Role.ACCOUNTANT);
    static final Set<Role> ACADEMICS = EnumSet.of(Role.OWNER, Role.PROPRIETOR, Role.PRINCIPAL, Role.SUPER_ADMIN,
            Role.ADMIN, Role.EXAM_OFFICER);
    public static final String MY_CLASSES = "myClasses";
    private static final int PASS_MARK = 50;
    // Subject marks count once the teacher has handed them in (not drafts still being typed), as the attention
    // reports do.
    private static final int MAX_NAMES = 10;

    private final JdbcTemplate jdbc;
    private final ModuleAccessService modules;
    private final SkullyReminderService numbers;
    private final SkullyInsightsService insights;
    private final GenerateStudentsRequiringAttentionUseCase studentsAttention;

    /** One school where this number belongs to staff, and what they may ask about there. */
    public record StaffAccess(String schoolId, String schoolName, String name, List<String> roles, List<String> canSee) {
    }

    private record Membership(String userId, String name, String schoolId, String schoolName, Role role) {
    }

    // ---------------------------------------------------------------------------------------------------------
    // The number itself (profile and user management)

    public String phone(String userId) {
        return jdbc.queryForList("SELECT phone FROM users WHERE id = ?", String.class, userId).stream()
                .findFirst().orElse(null);
    }

    /** Saves (or with a blank one, removes) a user's WhatsApp number. One account per number. */
    @Transactional
    public String savePhone(String userId, String phone) {
        String number = null;
        if (phone != null && !phone.isBlank()) {
            number = numbers.whatsappNumber(phone);
            if (number == null) throw new RuleException("That doesn't look like a phone number");
            Integer taken = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE phone = ? AND id <> ?",
                    Integer.class, number, userId);
            if (taken != null && taken > 0) {
                throw new AlreadyExistsException("Another account already uses this WhatsApp number");
            }
        }
        jdbc.update("UPDATE users SET phone = ?, updated_at = now() WHERE id = ?", number, userId);
        return number;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Skully

    public List<StaffAccess> staff(String phone) {
        Map<String, List<Membership>> bySchool = new LinkedHashMap<>();
        for (Membership m : memberships(phone)) bySchool.computeIfAbsent(m.schoolId(), k -> new ArrayList<>()).add(m);
        List<StaffAccess> out = new ArrayList<>();
        for (var entry : bySchool.entrySet()) {
            var first = entry.getValue().get(0);
            Set<Role> roles = roles(entry.getValue());
            out.add(new StaffAccess(first.schoolId(), first.schoolName(), first.name(),
                    STAFF_ROLES.stream().filter(roles::contains).map(Role::name).toList(), parts(roles).stream().toList()));
        }
        return out;
    }

    /** The school as this staff member's role sees it (not their classes - see myClasses). Empty if not theirs. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> overview(String phone, String schoolId) {
        List<Membership> mine = mine(phone, schoolId);
        if (mine.isEmpty()) return Optional.empty();
        Set<Role> roles = roles(mine);
        Set<String> parts = new LinkedHashSet<>(parts(roles));
        parts.remove(MY_CLASSES);
        Map<String, Object> out = new LinkedHashMap<>();
        if (!parts.isEmpty()) {
            Role actingAs = STAFF_ROLES.stream().filter(roles::contains).findFirst().orElseThrow();
            out.putAll(as(new AuthUser(mine.get(0).userId(), schoolId, actingAs), () -> insights.facts(schoolId, parts)));
        } else {
            out.put("note", "Their role doesn't include school-wide reports; only their own classes.");
        }
        out.put("yourRoles", STAFF_ROLES.stream().filter(roles::contains).map(Role::name).toList());
        out.put("youCanSee", parts);
        return Optional.of(out);
    }

    /** A teacher's own classes: the class they are class teacher of, and the subjects they teach. Empty if not a teacher there. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> myClasses(String phone, String schoolId) {
        List<Membership> mine = mine(phone, schoolId).stream().filter(m -> m.role() == Role.TEACHER).toList();
        if (mine.isEmpty()) return Optional.empty();
        String userId = mine.get(0).userId();
        String teacherId = jdbc.queryForList("""
                SELECT id FROM teachers WHERE user_id = ? AND school_id = ? AND status = 'ACTIVE'
                """, String.class, userId, schoolId).stream().findFirst().orElse(null);
        if (teacherId == null) return Optional.empty();

        LocalDate today = LocalDate.now(SchoolTimeZone.ZONE);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("today", today.toString());
        out.put("asClassTeacher", as(new AuthUser(userId, schoolId, Role.TEACHER),
                () -> classTeacher(schoolId, teacherId, monday, today)));
        out.put("asSubjectTeacher", subjectTeacher(schoolId, teacherId));
        return Optional.of(out);
    }

    // The class sections they are class teacher of this year: attendance and the students needing attention there.
    private List<Map<String, Object>> classTeacher(String schoolId, String teacherId, LocalDate monday, LocalDate today) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var cs : jdbc.queryForList("""
                SELECT cs.id, cs.class_id, cs.section_id, cs.stream_id, cs.academic_year_id, c.name AS class_name,
                       sec.name AS section_name, str.name AS stream_name
                FROM class_masters cm
                JOIN class_sessions cs ON cs.id = cm.class_session_id
                JOIN academic_years ay ON ay.id = cs.academic_year_id AND ay.active = TRUE
                JOIN classes c ON c.id = cs.class_id
                LEFT JOIN sections sec ON sec.id = cs.section_id
                LEFT JOIN streams str ON str.id = cs.stream_id
                WHERE cm.teacher_id = ? AND cm.school_id = ? AND cm.ended_at IS NULL
                ORDER BY c.name, sec.name
                """, teacherId, schoolId)) {
            // Only this class section's students (not the class's other sections or streams).
            Set<String> enrollments = new HashSet<>(jdbc.queryForList("""
                    SELECT id FROM enrollments
                    WHERE school_id = ? AND class_id = ? AND section_id = ? AND stream_id IS NOT DISTINCT FROM ?
                      AND academic_year_id = ? AND status = 'ACTIVE'
                    """, String.class, schoolId, cs.get("class_id"), cs.get("section_id"), cs.get("stream_id"),
                    cs.get("academic_year_id")));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("class", label(cs));
            m.put("students", enrollments.size());
            m.put("thisWeekPresentPercent", rate(enrollments, monday, today));
            m.put("lastWeekPresentPercent", rate(enrollments, monday.minusWeeks(1), monday.minusDays(1)));
            List<Map<String, Object>> flagged = new ArrayList<>();
            try {
                var res = studentsAttention.execute(schoolId, (String) cs.get("class_id"), null, null, null, 0, 500);
                for (var s : res.students()) {
                    if (!enrollments.contains(s.enrollmentId())) continue;
                    Map<String, Object> f = new LinkedHashMap<>();
                    f.put("name", (Optional.ofNullable(s.givenNames()).orElse("") + " "
                            + Optional.ofNullable(s.familyName()).orElse("")).trim());
                    f.put("academicAverage", s.academicAverage());
                    f.put("attendanceRate", s.attendanceRate());
                    f.put("assessmentsMissing", s.assessmentsMissing());
                    List<String> flags = new ArrayList<>();
                    if (s.lowAcademicSignal()) flags.add("low average");
                    if (s.lowAttendanceSignal()) flags.add("low attendance");
                    if (s.missingAssessmentsSignal()) flags.add("missing assessments");
                    if (s.decliningTrendSignal()) flags.add("declining");
                    f.put("flags", flags);
                    flagged.add(f);
                }
            } catch (RuntimeException e) {
                m.put("studentsNeedingAttentionError", "not available");
            }
            m.put("studentsNeedingAttention", flagged.stream().limit(15).toList());
            m.put("studentsNeedingAttentionCount", flagged.size());
            out.add(m);
        }
        return out;
    }

    // The subjects they teach, per class: this term's average in that subject and who is below the pass mark.
    private List<Map<String, Object>> subjectTeacher(String schoolId, String teacherId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var ts : jdbc.queryForList("""
                SELECT ts.id, s.name AS subject, c.name AS class_name, sec.name AS section_name, str.name AS stream_name
                FROM teacher_subjects ts
                JOIN class_sessions cs ON cs.id = ts.class_session_id
                JOIN academic_years ay ON ay.id = cs.academic_year_id AND ay.active = TRUE
                JOIN subjects s ON s.id = ts.subject_id
                JOIN classes c ON c.id = cs.class_id
                LEFT JOIN sections sec ON sec.id = cs.section_id
                LEFT JOIN streams str ON str.id = cs.stream_id
                WHERE ts.teacher_id = ? AND ts.school_id = ?
                ORDER BY c.name, sec.name, s.name
                """, teacherId, schoolId)) {
            var students = jdbc.queryForList("""
                    WITH m AS (
                        SELECT sa.enrollment_id, lc.term_id, t.name AS term, t.start_date, sc.score
                        FROM assessment_scores sc
                        JOIN class_subject_assessment_life_cycle lc ON lc.id = sc.class_subject_assessment_life_cycle_id
                            AND lc.teacher_subject_id = ?
                            AND lc.status IN ('SUBMITTED', 'RETURNED', 'APPROVED', 'COMPLETED', 'LOCKED')
                        JOIN terms t ON t.id = lc.term_id
                        JOIN student_assessments sa ON sa.id = sc.student_assessment_id
                    ), latest_term AS (SELECT term_id FROM m ORDER BY start_date DESC LIMIT 1)
                    SELECT m.term, st.given_names, st.family_name, ROUND(AVG(m.score)) AS average
                    FROM m
                    JOIN enrollments e ON e.id = m.enrollment_id AND e.status = 'ACTIVE'
                    JOIN students st ON st.id = e.student_id AND st.status = 'ACTIVE'
                    WHERE m.term_id = (SELECT term_id FROM latest_term)
                    GROUP BY m.term, st.id, st.given_names, st.family_name
                    ORDER BY average, st.given_names
                    """, ts.get("id"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subject", ts.get("subject"));
            m.put("class", label(ts));
            if (students.isEmpty()) {
                m.put("marks", "No marks handed in yet this term");
            } else {
                m.put("term", students.get(0).get("term"));
                m.put("studentsMarked", students.size());
                m.put("classAverage", Math.round(students.stream()
                        .mapToDouble(r -> ((Number) r.get("average")).doubleValue()).average().orElse(0)));
                var below = students.stream().filter(r -> ((Number) r.get("average")).intValue() < PASS_MARK).toList();
                m.put("belowPassMark", below.size());
                m.put("lowest", below.stream().limit(MAX_NAMES).map(r -> Map.of(
                        "name", (Optional.ofNullable((String) r.get("given_names")).orElse("") + " "
                                + Optional.ofNullable((String) r.get("family_name")).orElse("")).trim(),
                        "average", ((Number) r.get("average")).intValue())).toList());
            }
            out.add(m);
        }
        return out;
    }

    private Long rate(Set<String> enrollments, LocalDate from, LocalDate to) {
        if (enrollments.isEmpty()) return null;
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(from), Date.valueOf(to)));
        args.addAll(enrollments);
        Double r = jdbc.queryForObject("""
                SELECT 100.0 * COUNT(*) FILTER (WHERE present) / NULLIF(COUNT(*), 0)
                FROM attendance WHERE date BETWEEN ? AND ? AND NOT holiday AND enrollment_id IN (""" +
                String.join(",", enrollments.stream().map(e -> "?").toList()) + ")", Double.class, args.toArray());
        return r == null ? null : Math.round(r);
    }

    private static String label(Map<String, Object> row) {
        StringBuilder s = new StringBuilder(String.valueOf(row.get("class_name")));
        for (String k : List.of("section_name", "stream_name")) {
            Object v = row.get(k);
            if (v != null && !v.toString().isBlank()) s.append(' ').append(v);
        }
        return s.toString();
    }

    static Set<String> parts(Set<Role> roles) {
        Set<String> parts = new LinkedHashSet<>();
        if (roles.stream().anyMatch(ACADEMICS::contains)) {
            parts.add(SkullyInsightsService.ATTENDANCE);
            parts.add(SkullyInsightsService.ACADEMICS);
        }
        if (roles.stream().anyMatch(FEES::contains)) parts.add(SkullyInsightsService.FEES);
        if (roles.contains(Role.TEACHER)) parts.add(MY_CLASSES);
        return parts;
    }

    private static Set<Role> roles(List<Membership> memberships) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        memberships.forEach(m -> roles.add(m.role()));
        return roles;
    }

    private List<Membership> mine(String phone, String schoolId) {
        return memberships(phone).stream().filter(m -> m.schoolId().equals(schoolId)).toList();
    }

    // Active staff memberships for this number, at schools with Skully on. The number must be the whole number
    // (with country code) on their account, or on their teacher profile at that school - never just its last digits.
    // If it would match two different accounts, nobody gets staff access through it: school data must not go to
    // the wrong person.
    List<Membership> memberships(String phone) {
        String number = numbers.whatsappNumber(phone);
        if (number == null) return List.of();
        String tail = number.substring(Math.max(0, number.length() - 8));
        List<Membership> byAccount = new ArrayList<>(), byTeacherProfile = new ArrayList<>();
        for (var r : jdbc.queryForList("""
                SELECT u.id, u.given_name, u.family_name, u.phone AS user_phone, t.phone AS teacher_phone,
                       su.school_id, su.role, s.name AS school_name
                FROM users u
                JOIN school_users su ON su.user_id = u.id AND su.status = 'ACTIVE'
                JOIN schools s ON s.id = su.school_id
                LEFT JOIN teachers t ON t.user_id = u.id AND t.school_id = su.school_id AND t.status = 'ACTIVE'
                WHERE u.status IN ('ACTIVE', 'RESET_PASSWORD')
                  AND (u.phone = ? OR right(regexp_replace(t.phone, '[^0-9]', '', 'g'), 8) = ?)
                ORDER BY s.name
                """, number, tail)) {
            boolean account = number.equals(r.get("user_phone"));
            boolean teacherProfile = number.equals(numbers.whatsappNumber((String) r.get("teacher_phone")));
            if (!account && !teacherProfile) continue;
            Role role;
            try {
                role = Role.valueOf((String) r.get("role"));
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!STAFF_ROLES.contains(role)) continue;
            String schoolId = (String) r.get("school_id");
            if (!modules.isEnabled(schoolId, FeatureModule.WHATSAPP_ASSISTANT)) continue;
            String name = (Optional.ofNullable((String) r.get("given_name")).orElse("") + " "
                    + Optional.ofNullable((String) r.get("family_name")).orElse("")).trim();
            (account ? byAccount : byTeacherProfile)
                    .add(new Membership((String) r.get("id"), name, schoolId, (String) r.get("school_name"), role));
        }
        // The number saved on an account wins; a teacher-profile phone only counts when no account claims it.
        List<Membership> chosen = byAccount.isEmpty() ? byTeacherProfile : byAccount;
        long people = chosen.stream().map(Membership::userId).distinct().count();
        return people == 1 ? chosen : List.of();
    }

    // Runs the reports as this staff member, so the same section scope applies as when they sign in.
    private static <T> T as(AuthUser user, Supplier<T> work) {
        var context = SecurityContextHolder.getContext();
        var previous = context.getAuthentication();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        try {
            return work.get();
        } finally {
            context.setAuthentication(previous);
        }
    }
}
