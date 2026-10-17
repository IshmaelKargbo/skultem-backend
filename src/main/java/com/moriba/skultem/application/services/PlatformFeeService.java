package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The platform fee: a flat amount per student per term, the same for every school. A student is charged it once per
 * academic year (see SeedPlatformFeeForAcademicYearUseCase), so what is stored is the whole year's amount - the
 * per-term figure times {@link #TERMS_PER_YEAR}. Changing it affects fees seeded from then on; what students were
 * already charged is never rewritten.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PlatformFeeService {

    public static final int TERMS_PER_YEAR = 3;
    private static final BigDecimal MAX_PER_TERM = new BigDecimal("1000000");

    public record Overview(BigDecimal perTerm, int termsPerYear, BigDecimal perYear, long schools, long schoolsOnThisFee,
            long schoolsOnOtherFees, Instant updatedAt) {
    }

    public record SchoolCollection(String schoolId, String name, String domain, String status, long students,
            BigDecimal billed, BigDecimal discounts, BigDecimal collected, BigDecimal outstanding, int rate,
            Instant lastPaymentAt) {
    }

    public record Collections(BigDecimal billed, BigDecimal discounts, BigDecimal collected, BigDecimal outstanding,
            int rate, long students, long schools, long schoolsOwing, java.util.List<SchoolCollection> items) {
    }

    // Where the platform's fee per term is kept (platform_config), so it is one stated value rather than something
    // worked out from what individual schools happen to have.
    static final String KEY = "platform.feePerTerm";

    private final JdbcTemplate jdbc;

    /** What a new school's platform fee starts at - the platform's fee for a whole year - or null when none has been set. */
    public BigDecimal feePerYearForNewSchools() {
        var stored = jdbc.queryForList("SELECT value FROM platform_config WHERE key = ?", String.class, KEY);
        if (stored.isEmpty()) {
            return null;
        }
        return new BigDecimal(stored.get(0)).multiply(BigDecimal.valueOf(TERMS_PER_YEAR)).setScale(2, RoundingMode.HALF_UP);
    }

    /** The fee most schools are on (the platform's fee), and how many schools differ from it. */
    public Overview overview() {
        long schools = count("SELECT count(*) FROM schools WHERE status <> 'DELETED'");
        BigDecimal stated = feePerYearForNewSchools();
        var modes = jdbc.queryForList("""
                SELECT platform_fee_amount AS amount, count(*) AS n FROM platform_settings
                WHERE platform_fee_amount IS NOT NULL AND platform_fee_amount > 0
                GROUP BY platform_fee_amount ORDER BY n DESC, platform_fee_amount DESC LIMIT 1
                """);
        // The stated platform fee wins; before one has been set it is whatever most schools already pay.
        BigDecimal perYear = stated != null ? stated
                : modes.isEmpty() ? new BigDecimal("150.00") : (BigDecimal) modes.get(0).get("amount");
        long onThis = count("SELECT count(*) FROM platform_settings s JOIN schools sc ON sc.id = s.id WHERE sc.status <> 'DELETED' AND s.platform_fee_amount = ?", perYear);
        var updated = jdbc.queryForList("SELECT max(updated_at) FROM (SELECT updated_at FROM platform_settings UNION ALL SELECT updated_at FROM platform_config WHERE key = '" + KEY + "') x", java.sql.Timestamp.class);
        var at = updated.isEmpty() || updated.get(0) == null ? null : Instant.ofEpochMilli(updated.get(0).getTime());
        return new Overview(perYear.divide(BigDecimal.valueOf(TERMS_PER_YEAR), 2, RoundingMode.HALF_UP), TERMS_PER_YEAR,
                perYear.setScale(2, RoundingMode.HALF_UP), schools, onThis, schools - onThis, at);
    }

    /** Sets the same fee for every school. */
    @AuditLogAnnotation(action = "PLATFORM_FEE_SET_FOR_ALL_SCHOOLS")
    public Overview setForAllSchools(BigDecimal perTerm) {
        if (perTerm == null || perTerm.signum() <= 0) {
            throw new RuleException("The platform fee must be more than 0");
        }
        if (perTerm.compareTo(MAX_PER_TERM) > 0) {
            throw new RuleException("That is more than the platform fee can be set to");
        }
        BigDecimal perYear = perTerm.multiply(BigDecimal.valueOf(TERMS_PER_YEAR)).setScale(2, RoundingMode.HALF_UP);

        jdbc.update("UPDATE platform_settings SET platform_fee_amount = ?, updated_at = now() WHERE platform_fee_amount IS DISTINCT FROM ?",
                perYear, perYear);
        jdbc.update("""
                INSERT INTO platform_config (key, value, updated_at) VALUES (?, ?, now())
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_at = now()
                """, KEY, perTerm.setScale(2, RoundingMode.HALF_UP).toPlainString());
        // A school that has no setting yet gets one, so it isn't left without the platform fee.
        jdbc.update("""
                INSERT INTO platform_settings (id, platform_fee_amount, updated_at)
                SELECT s.id, ?, now() FROM schools s
                WHERE s.status <> 'DELETED' AND NOT EXISTS (SELECT 1 FROM platform_settings p WHERE p.id = s.id)
                """, perYear);
        return overview();
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    /**
     * What each live school owes Skultem in platform fees and how much of it students have already paid - read from the
     * schools' own fee records: the "Platform Fee" charged to each student (less any discount they were given) against
     * the payments recorded for it. Playground schools are left out; their fees are test data.
     */
    @Transactional
    public Collections collections() {
        var rows = jdbc.queryForList("""
                WITH charged AS (
                    SELECT sf.school_id, sf.student_id, fs.amount AS gross,
                           COALESCE(CASE d.kind
                                      WHEN 'PERCENTAGE' THEN fs.amount * LEAST(d.value, 100) / 100
                                      WHEN 'AMOUNT' THEN LEAST(d.value, fs.amount) END, 0) AS discount
                    FROM student_fees sf
                    JOIN fee_structures fs ON fs.id = sf.fee_id AND fs.system
                    LEFT JOIN fee_discounts d ON d.enrollment_id = sf.enrollment_id AND d.fee_id = sf.fee_id
                ), billed AS (
                    SELECT school_id, count(DISTINCT student_id) AS students, COALESCE(sum(gross), 0) AS gross,
                           COALESCE(sum(discount), 0) AS discount
                    FROM charged GROUP BY school_id
                ), paid AS (
                    SELECT p.school_id, COALESCE(sum(p.amount), 0) AS collected, max(p.paid_at) AS last_paid
                    FROM payments p JOIN fee_structures fs ON fs.id = p.fee_id AND fs.system
                    GROUP BY p.school_id
                )
                SELECT sc.id, sc.name, sc.domain, sc.status,
                       COALESCE(b.students, 0) AS students, COALESCE(b.gross, 0) AS gross,
                       COALESCE(b.discount, 0) AS discount, COALESCE(p.collected, 0) AS collected, p.last_paid
                FROM schools sc
                LEFT JOIN billed b ON b.school_id = sc.id
                LEFT JOIN paid p ON p.school_id = sc.id
                WHERE sc.status <> 'DELETED' AND NOT sc.is_test_school
                ORDER BY sc.name
                """);
        var items = new java.util.ArrayList<SchoolCollection>();
        BigDecimal billed = BigDecimal.ZERO, discounts = BigDecimal.ZERO, collected = BigDecimal.ZERO,
                outstanding = BigDecimal.ZERO;
        long students = 0, owing = 0;
        for (var r : rows) {
            BigDecimal gross = (BigDecimal) r.get("gross");
            BigDecimal discount = (BigDecimal) r.get("discount");
            BigDecimal paid = (BigDecimal) r.get("collected");
            BigDecimal due = gross.subtract(discount);
            BigDecimal left = due.subtract(paid).max(BigDecimal.ZERO);
            long n = ((Number) r.get("students")).longValue();
            var last = (java.sql.Timestamp) r.get("last_paid");
            items.add(new SchoolCollection((String) r.get("id"), (String) r.get("name"), (String) r.get("domain"),
                    (String) r.get("status"), n, due, discount, paid, left, rate(paid, due),
                    last == null ? null : last.toInstant()));
            billed = billed.add(due);
            discounts = discounts.add(discount);
            collected = collected.add(paid);
            outstanding = outstanding.add(left);
            students += n;
            if (left.signum() > 0) {
                owing++;
            }
        }
        return new Collections(billed, discounts, collected, outstanding, rate(collected, billed), students,
                items.size(), owing, items);
    }

    private static int rate(BigDecimal paid, BigDecimal due) {
        return due.signum() <= 0 ? 0 : paid.multiply(BigDecimal.valueOf(100)).divide(due, 0, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100)).intValue();
    }
}
