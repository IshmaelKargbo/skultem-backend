package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BillingDTO;
import com.moriba.skultem.application.dto.SystemInsightsDTO;
import com.moriba.skultem.application.dto.SystemInsightsDTO.ActivityRow;
import com.moriba.skultem.application.dto.SystemInsightsDTO.Content;
import com.moriba.skultem.application.dto.SystemInsightsDTO.RecentSchool;
import com.moriba.skultem.application.dto.SystemInsightsDTO.Delta;
import com.moriba.skultem.application.dto.SystemInsightsDTO.Demos;
import com.moriba.skultem.application.dto.SystemInsightsDTO.Growth;
import com.moriba.skultem.application.dto.SystemInsightsDTO.MonthPoint;
import com.moriba.skultem.application.dto.SystemInsightsDTO.SchoolRow;
import com.moriba.skultem.application.dto.SystemInsightsDTO.Schools;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Platform-wide numbers for the system admin's dashboard. These cut across every school, so they are read
 * straight from the tables in a handful of aggregate queries rather than loading anything school by school.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SystemInsightsUseCase {

    private static final java.util.Set<Integer> PERIODS = java.util.Set.of(7, 30, 90, 365);
    private static final int LIST_SIZE = 5;

    private final JdbcTemplate jdbc;
    private final com.moriba.skultem.application.services.LeadService leadService;
    private final com.moriba.skultem.application.services.OnboardingService onboardingService;
    private final com.moriba.skultem.application.services.BillingService billingService;
    private final com.moriba.skultem.application.services.SupportService supportService;
    private final com.moriba.skultem.infrastructure.security.PermissionService permissions;
    private final com.moriba.skultem.application.services.PlatformSettingsService settings;

    public SystemInsightsDTO execute(int requestedDays) {
        // Only the periods the dashboard offers - anything else falls back to 30 days.
        int days = PERIODS.contains(requestedDays) ? requestedDays : 30;
        int months = days >= 365 ? 12 : 6;

        // Each part of the overview is only worked out - and only sent - for an admin whose platform role covers it,
        // so the dashboard can never show someone figures they could not open the page for.
        boolean schools = permissions.hasPlatformPermission("SCHOOLS");
        boolean marketing = permissions.hasPlatformPermission("MARKETING");
        boolean billing = permissions.hasPlatformPermission("BILLING");
        boolean audit = permissions.hasPlatformPermission("AUDIT");

        return new SystemInsightsDTO(days, schools(), growth(days), monthly(months),
                schools ? topSchools() : List.of(), schools ? needsAttention() : List.of(),
                marketing ? demos(days) : null, marketing ? content() : null,
                schools ? recentSchools() : List.of(), audit ? recentActivity() : List.of(),
                marketing ? leads() : null, marketing ? onboarding() : null,
                billing ? billing(days) : null,
                billing && billingService.configured() ? billingService.renewals(30).stream().limit(5).toList() : List.of(),
                permissions.hasPlatformPermission("SUPPORT") ? support() : null);
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private Schools schools() {
        long live = count("SELECT count(*) FROM schools WHERE status = 'ACTIVE' AND NOT is_test_school");
        long playground = count("SELECT count(*) FROM schools WHERE status = 'ACTIVE' AND is_test_school");
        long inactive = count("SELECT count(*) FROM schools WHERE status = 'INACTIVE'");
        return new Schools(live + playground + inactive, live, playground, inactive);
    }

    // `table` and `where` are fixed strings from this class, never user input; days is one of PERIODS.
    private Delta delta(String table, String where, int days) {
        String w = where == null ? "" : " AND " + where;
        long current = count("SELECT count(*) FROM " + table
                + " WHERE created_at >= now() - make_interval(days => ?)" + w, days);
        long previous = count("SELECT count(*) FROM " + table
                + " WHERE created_at >= now() - make_interval(days => ?) AND created_at < now() - make_interval(days => ?)" + w,
                days * 2, days);
        return new Delta(current, previous);
    }

    private Growth growth(int days) {
        return new Growth(
                delta("schools", "status <> 'DELETED'", days),
                delta("students", "status <> 'DELETED'", days),
                delta("users", null, days),
                delta("request_demos", null, days));
    }

    // One row per calendar month, oldest first, including months where nothing happened.
    private List<MonthPoint> monthly(int months) {
        return jdbc.query("""
                SELECT to_char(m.month, 'YYYY-MM') AS month,
                       (SELECT count(*) FROM schools s
                         WHERE s.status <> 'DELETED' AND date_trunc('month', s.created_at) = m.month) AS schools,
                       (SELECT count(*) FROM students st
                         WHERE st.status <> 'DELETED' AND date_trunc('month', st.created_at) = m.month) AS students,
                       (SELECT count(*) FROM request_demos d
                         WHERE date_trunc('month', d.created_at) = m.month) AS demos
                FROM generate_series(date_trunc('month', now()) - make_interval(months => ?),
                                     date_trunc('month', now()), interval '1 month') AS m(month)
                ORDER BY m.month
                """,
                (rs, i) -> new MonthPoint(rs.getString("month"), rs.getLong("schools"), rs.getLong("students"),
                        rs.getLong("demos")),
                months - 1);
    }

    private List<SchoolRow> topSchools() {
        return jdbc.query("""
                SELECT s.id, s.name, s.domain, s.is_test_school, s.created_at,
                       count(st.id) FILTER (WHERE st.status = 'ACTIVE') AS students
                FROM schools s LEFT JOIN students st ON st.school_id = s.id
                WHERE s.status = 'ACTIVE'
                GROUP BY s.id
                ORDER BY students DESC, s.name
                LIMIT ?
                """,
                (rs, i) -> row(rs, null), LIST_SIZE);
    }

    // Schools that are set up but not really being used: live with nobody enrolled, or stuck in playground.
    private List<SchoolRow> needsAttention() {
        // A school still in playground mode this long is probably stuck rather than being tried out (a platform setting).
        int staleDays = settings.intValue(com.moriba.skultem.application.services.PlatformSettingsService.STALE_PLAYGROUND_DAYS);
        return jdbc.query("""
                SELECT s.id, s.name, s.domain, s.is_test_school, s.created_at,
                       count(st.id) FILTER (WHERE st.status = 'ACTIVE') AS students
                FROM schools s LEFT JOIN students st ON st.school_id = s.id
                WHERE s.status = 'ACTIVE'
                GROUP BY s.id
                HAVING (NOT s.is_test_school AND count(st.id) FILTER (WHERE st.status = 'ACTIVE') = 0)
                    OR (s.is_test_school AND s.created_at < now() - make_interval(days => ?))
                ORDER BY s.created_at
                LIMIT ?
                """,
                (rs, i) -> row(rs, rs.getBoolean("is_test_school")
                        ? "In playground mode for over " + staleDays + " days"
                        : "Live, but no students enrolled"),
                staleDays, LIST_SIZE);
    }

    private SchoolRow row(java.sql.ResultSet rs, String reason) throws java.sql.SQLException {
        var created = rs.getTimestamp("created_at");
        return new SchoolRow(rs.getString("id"), rs.getString("name"), rs.getString("domain"), rs.getLong("students"),
                rs.getBoolean("is_test_school"), reason, created == null ? null : Instant.ofEpochMilli(created.getTime()));
    }

    private SystemInsightsDTO.Leads leads() {
        var s = leadService.summary();
        long open = s.total() - s.byStatus().getOrDefault("CONVERTED", 0L) - s.byStatus().getOrDefault("LOST", 0L);
        return new SystemInsightsDTO.Leads(open, s.awaitingFollowUp(), s.unassigned(), s.overdue());
    }

    private SystemInsightsDTO.Support support() {
        var s = supportService.summary();
        // Tickets still needing work: everything that is neither resolved nor closed.
        return new SystemInsightsDTO.Support(s.open() + s.inProgress() + s.waiting(), s.unassigned(), s.urgent());
    }

    private SystemInsightsDTO.Billing billing(int days) {
        if (!billingService.configured()) {
            return null;
        }
        var s = billingService.summary(days);
        return new SystemInsightsDTO.Billing(s.currency(), s.activeSubscriptions(), s.revenueInPeriod(), s.outstanding(),
                s.overdueAmount(), s.overdueInvoices(), s.expiringSoon());
    }

    private SystemInsightsDTO.Onboarding onboarding() {
        var o = onboardingService.overview();
        return new SystemInsightsDTO.Onboarding(o.get("inProgress"), o.get("completed"), o.get("overdueSteps"));
    }

    private Demos demos(int days) {
        return new Demos(count("SELECT count(*) FROM request_demos"),
                count("SELECT count(*) FROM request_demos WHERE created_at >= now() - make_interval(days => ?)", days));
    }

    // The newest schools, with who owns them (the owner is stored as JSON on the school).
    private List<RecentSchool> recentSchools() {
        return jdbc.query("""
                SELECT id, name, domain, status, is_test_school, created_at,
                       trim(concat_ws(' ', owner ->> 'givenNames', owner ->> 'familyName')) AS owner_name
                FROM schools
                WHERE status <> 'DELETED'
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, i) -> new RecentSchool(rs.getString("id"), rs.getString("name"), rs.getString("domain"),
                        rs.getString("status"), rs.getBoolean("is_test_school"),
                        blankToNull(rs.getString("owner_name")), instant(rs.getTimestamp("created_at"))),
                LIST_SIZE);
    }

    // The latest recorded actions across every school, from the audit log. Read-only.
    private List<ActivityRow> recentActivity() {
        return jdbc.query("""
                SELECT a.id, a.action, a.status, a.created_at, s.name AS school_name,
                       trim(concat_ws(' ', u.given_name, u.family_name)) AS user_name
                FROM audit_logs a
                LEFT JOIN schools s ON s.id = a.school_id
                LEFT JOIN users u ON u.id = a.user_id
                ORDER BY a.created_at DESC
                LIMIT 8
                """,
                (rs, i) -> new ActivityRow(rs.getString("id"), rs.getString("action"), rs.getString("status"),
                        rs.getString("school_name"), blankToNull(rs.getString("user_name")),
                        instant(rs.getTimestamp("created_at"))));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static Instant instant(java.sql.Timestamp t) {
        return t == null ? null : Instant.ofEpochMilli(t.getTime());
    }

    private Content content() {
        return new Content(
                count("SELECT count(*) FROM marketing_contact_messages WHERE status = 'NEW'"),
                count("SELECT count(*) FROM marketing_blog_posts WHERE published"),
                count("SELECT count(*) FROM marketing_blog_posts WHERE NOT published"),
                count("SELECT count(*) FROM marketing_testimonials WHERE published"));
    }
}
