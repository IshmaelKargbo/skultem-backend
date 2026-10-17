package com.moriba.skultem.application.services;

import java.lang.management.ManagementFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The platform's operational picture: how the system is doing right now, and what notifications have gone out.
 *
 * It only reports what Skultem can actually know. Where nothing is measured - whether an email or text reached a
 * person, whether a background job failed - the answer is "not tracked", never a green tick, and no figure here is
 * a guess: a message is only ever described as recorded or composed, never as delivered.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PlatformOperationsService {

    public enum Level {
        OK, WARNING, FAILED, NOT_TRACKED
    }

    public record Check(String key, String label, Level level, String detail, Long latencyMs) {
    }

    public record RuntimeInfo(String java, long uptimeSeconds, long memoryUsedMb, long memoryMaxMb, int processors) {
    }

    public record Health(Level overall, Instant checkedAt, List<Check> checks, RuntimeInfo runtime) {
    }

    private final JdbcTemplate jdbc;
    private final R2StorageService storage;

    @Value("${mail.mode:dev}")
    private String mailMode;

    // ------------------------------------------------------------------ health

    public Health health() {
        var checks = new ArrayList<Check>();
        checks.add(api());
        checks.add(database());
        checks.add(migrations());
        checks.add(storage());
        checks.add(recentFailures());
        checks.add(new Check("email", "Email delivery", Level.NOT_TRACKED,
                "Mail mode is '" + mailMode + "'. Whether a message reached its recipient isn't recorded, so no delivery figures are shown.", null));
        checks.add(new Check("jobs", "Background jobs", Level.NOT_TRACKED,
                "No scheduled jobs are configured, and one-off background tasks don't record failures.", null));

        Level overall = Level.OK;
        for (var c : checks) {
            if (c.level() == Level.FAILED) {
                overall = Level.FAILED;
                break;
            }
            if (c.level() == Level.WARNING) {
                overall = Level.WARNING;
            }
        }

        var memory = Runtime.getRuntime();
        return new Health(overall, Instant.now(), checks,
                new RuntimeInfo(System.getProperty("java.version"), ManagementFactory.getRuntimeMXBean().getUptime() / 1000,
                        (memory.totalMemory() - memory.freeMemory()) / 1_048_576, memory.maxMemory() / 1_048_576,
                        memory.availableProcessors()));
    }

    private Check api() {
        long seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return new Check("api", "Backend", Level.OK, "Responding. Up for " + duration(seconds) + ".", null);
    }

    private Check database() {
        long start = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new Check("database", "Database", ms > 500 ? Level.WARNING : Level.OK,
                    ms > 500 ? "Connected, but slow to answer." : "Connected.", ms);
        } catch (RuntimeException e) {
            return new Check("database", "Database", Level.FAILED, "Could not run a query.", null);
        }
    }

    private Check migrations() {
        try {
            var rows = jdbc.queryForList("SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC");
            long failed = rows.stream().filter(r -> Boolean.FALSE.equals(r.get("success"))).count();
            String latest = rows.isEmpty() ? "none" : String.valueOf(rows.get(0).get("version"));
            return failed > 0
                    ? new Check("migrations", "Database migrations", Level.FAILED, failed + " migration(s) failed. Latest version: " + latest + ".", null)
                    : new Check("migrations", "Database migrations", Level.OK, "Up to date at version " + latest + ".", null);
        } catch (RuntimeException e) {
            return new Check("migrations", "Database migrations", Level.NOT_TRACKED, "The migration history couldn't be read.", null);
        }
    }

    private Check storage() {
        try {
            long ms = storage.ping();
            return new Check("storage", "File storage", ms > 2000 ? Level.WARNING : Level.OK,
                    ms > 2000 ? "Reachable, but slow to answer." : "Reachable.", ms);
        } catch (RuntimeException e) {
            return new Check("storage", "File storage", Level.FAILED, "The storage bucket could not be reached - uploads and photos will fail.", null);
        }
    }

    // The nearest thing to an application error log: actions the audit trail recorded as having failed.
    private Check recentFailures() {
        try {
            long day = count("SELECT count(*) FROM audit_logs WHERE status = 'FAILURE' AND created_at >= now() - interval '24 hours'");
            if (day == 0) {
                return new Check("failures", "Failed actions (24 hours)", Level.OK, "No audited action failed in the last 24 hours.", null);
            }
            var top = jdbc.queryForList("""
                    SELECT action, count(*) AS n FROM audit_logs WHERE status = 'FAILURE' AND created_at >= now() - interval '24 hours'
                    GROUP BY action ORDER BY n DESC LIMIT 3""");
            var summary = new ArrayList<String>();
            for (var r : top) {
                summary.add(r.get("action") + " ×" + r.get("n"));
            }
            return new Check("failures", "Failed actions (24 hours)", day >= 20 ? Level.FAILED : Level.WARNING,
                    day + " audited action" + (day == 1 ? "" : "s") + " failed: " + String.join(", ", summary) + ". See the audit log.", null);
        } catch (RuntimeException e) {
            return new Check("failures", "Failed actions (24 hours)", Level.NOT_TRACKED, "The audit log couldn't be read.", null);
        }
    }

    static String duration(long seconds) {
        var d = Duration.ofSeconds(seconds);
        if (d.toDays() > 0) {
            return d.toDays() + "d " + d.toHoursPart() + "h";
        }
        if (d.toHours() > 0) {
            return d.toHours() + "h " + d.toMinutesPart() + "m";
        }
        return Math.max(d.toMinutes(), 0) + "m";
    }

    // ------------------------------------------------------------------ notification history

    /** A message a school composed from its Communicate page. delivery is not included on purpose - see the class note. */
    public record Broadcast(String id, String schoolId, String schoolName, String title, String audience,
            List<String> channels, String status, int recipients, String sentByName, Instant scheduledAt,
            Instant sentAt, Instant createdAt) {
    }

    public record TypeCount(String type, long created, long read) {
    }

    public record Overview(int days, long broadcasts, long scheduledBroadcasts, long inApp, long inAppRead,
            List<TypeCount> byType, Map<String, Long> announcements) {
    }

    public Overview overview(int days) {
        var byType = jdbc.query("""
                SELECT type, count(*) AS created, count(*) FILTER (WHERE read) AS read_count
                FROM notifications WHERE created_at >= now() - make_interval(days => ?)
                GROUP BY type ORDER BY created DESC
                """, (rs, i) -> new TypeCount(rs.getString("type"), rs.getLong("created"), rs.getLong("read_count")), days);

        var announcements = new LinkedHashMap<String, Long>();
        announcements.put("live", count("SELECT count(*) FROM platform_announcements WHERE active AND (starts_at IS NULL OR starts_at <= now()) AND (ends_at IS NULL OR ends_at > now())"));
        announcements.put("scheduled", count("SELECT count(*) FROM platform_announcements WHERE active AND starts_at > now()"));
        announcements.put("ended", count("SELECT count(*) FROM platform_announcements WHERE ends_at IS NOT NULL AND ends_at <= now()"));

        return new Overview(days,
                count("SELECT count(*) FROM broadcasts WHERE created_at >= now() - make_interval(days => ?)", days),
                count("SELECT count(*) FROM broadcasts WHERE status = 'SCHEDULED'"),
                byType.stream().mapToLong(TypeCount::created).sum(), byType.stream().mapToLong(TypeCount::read).sum(),
                byType, announcements);
    }

    public Page<Broadcast> broadcasts(String search, String schoolId, int page, int size) {
        var where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();
        if (search != null && !search.isBlank()) {
            where.append(" AND (lower(b.title) LIKE ? OR lower(s.name) LIKE ?)");
            String like = "%" + search.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
        }
        if (schoolId != null && !schoolId.isBlank()) {
            where.append(" AND b.school_id = ?");
            args.add(schoolId);
        }
        Long total = jdbc.queryForObject("SELECT count(*) FROM broadcasts b JOIN schools s ON s.id = b.school_id" + where, Long.class, args.toArray());
        var pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) Math.max(page - 1, 0) * size);
        var rows = jdbc.query("""
                SELECT b.id, b.school_id, s.name AS school_name, b.title, b.audience, b.channels, b.status,
                       b.recipients_count, b.sent_by_name, b.scheduled_at, b.sent_at, b.created_at
                FROM broadcasts b JOIN schools s ON s.id = b.school_id
                """ + where + " ORDER BY b.created_at DESC LIMIT ? OFFSET ?", (rs, i) -> mapBroadcast(rs), pageArgs.toArray());
        return new PageImpl<>(rows, PageRequest.of(Math.max(page - 1, 0), size), total == null ? 0 : total);
    }

    private Broadcast mapBroadcast(ResultSet rs) throws SQLException {
        String channels = rs.getString("channels");
        return new Broadcast(rs.getString("id"), rs.getString("school_id"), rs.getString("school_name"),
                rs.getString("title"), rs.getString("audience"),
                channels == null || channels.isBlank() ? List.of() : List.of(channels.split(",")),
                rs.getString("status"), rs.getInt("recipients_count"), rs.getString("sent_by_name"),
                instant(rs, "scheduled_at"), instant(rs, "sent_at"), instant(rs, "created_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var t = rs.getTimestamp(column);
        return t == null ? null : Instant.ofEpochMilli(t.getTime());
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }
}
