package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BillingDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.infrastructure.rest.dto.BillingRequests;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * What each school pays Skultem. A school has one subscription at a time; each billing period gets an invoice;
 * staff record the payments actually received against invoices. Whether a school is ACTIVE, PENDING, OVERDUE,
 * EXPIRED or CANCELLED is worked out from those invoices and today's date - never stored, never guessed.
 *
 * Nothing here ever changes a school or deletes its data: an expired or cancelled subscription is a billing
 * fact, not a reason to touch the school.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BillingService {

    public static final String CURRENCY = "SLE";

    public enum State {
        ACTIVE, PENDING, OVERDUE, EXPIRED, CANCELLED
    }

    private final JdbcTemplate jdbc;
    private final PlatformSettingsService settings;

    // ------------------------------------------------------------------ pure rules

    /** How many months one billing period lasts. */
    static int months(String frequency) {
        return switch (frequency) {
            case "MONTHLY" -> 1;
            case "QUARTERLY" -> 3;
            case "ANNUAL" -> 12;
            default -> throw new RuleException("Unknown billing frequency: " + frequency);
        };
    }

    /** The last day of a period that starts on {@code start}: one frequency later, less a day. */
    public static LocalDate periodEnd(LocalDate start, String frequency) {
        return start.plusMonths(months(frequency)).minusDays(1);
    }

    /**
     * Where a subscription stands today. A cancelled one is cancelled; any invoice past its due date and not
     * fully paid makes it overdue; a fully paid invoice covering today makes it active; once the invoiced
     * periods have run out (and are paid) it has expired; anything else is waiting on its first payment or start.
     */
    public static State stateOf(boolean cancelled, boolean hasOverdue, boolean coveredByPaidInvoice,
            LocalDate currentPeriodEnd, LocalDate today) {
        if (cancelled) {
            return State.CANCELLED;
        }
        if (hasOverdue) {
            return State.OVERDUE;
        }
        if (coveredByPaidInvoice) {
            return State.ACTIVE;
        }
        if (currentPeriodEnd.isBefore(today)) {
            return State.EXPIRED;
        }
        return State.PENDING;
    }

    // ------------------------------------------------------------------ plans

    @AuditLogAnnotation(action = "SUBSCRIPTION_PLAN_SAVED")
    public BillingDTO.Plan savePlan(String id, BillingRequests.SavePlan p) {
        // Checked first (rather than waiting for the database to refuse): a plan name is unique whatever its case.
        Long same = id == null
                ? jdbc.queryForObject("SELECT count(*) FROM subscription_plans WHERE lower(name) = lower(?)", Long.class, p.name().trim())
                : jdbc.queryForObject("SELECT count(*) FROM subscription_plans WHERE lower(name) = lower(?) AND id <> ?", Long.class, p.name().trim(), LeadService.parse(id));
        if (same != null && same > 0) {
            throw new AlreadyExistsException("A plan named '" + p.name().trim() + "' already exists");
        }

        var now = java.sql.Timestamp.from(Instant.now());
        UUID planId;
        if (id == null) {
            planId = UUID.randomUUID();
            insertPlan(planId, p, now);
        } else {
            planId = LeadService.parse(id);
            int updated = jdbc.update("""
                    UPDATE subscription_plans SET name = ?, description = ?, price = ?, frequency = ?, max_students = ?,
                           active = ?, updated_at = ? WHERE id = ?
                    """, p.name().trim(), blank(p.description()), p.price(), p.frequency(), p.maxStudents(), p.active(), now, planId);
            if (updated == 0) {
                throw new NotFoundException("Plan not found");
            }
        }
        return plan(planId);
    }

    private void insertPlan(UUID id, BillingRequests.SavePlan p, java.sql.Timestamp now) {
        try {
            jdbc.update("""
                    INSERT INTO subscription_plans (id, name, description, price, currency, frequency, max_students, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, p.name().trim(), blank(p.description()), p.price(), CURRENCY, p.frequency(), p.maxStudents(), p.active(), now, now);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new AlreadyExistsException("A plan named '" + p.name().trim() + "' already exists");
        }
    }

    public List<BillingDTO.Plan> plans() {
        return jdbc.query(PLAN_SELECT + " ORDER BY p.active DESC, p.price", (rs, i) -> mapPlan(rs));
    }

    private BillingDTO.Plan plan(UUID id) {
        var rows = jdbc.query(PLAN_SELECT + " WHERE p.id = ?", (rs, i) -> mapPlan(rs), id);
        if (rows.isEmpty()) {
            throw new NotFoundException("Plan not found");
        }
        return rows.get(0);
    }

    private static final String PLAN_SELECT = """
            SELECT p.*, (SELECT count(*) FROM school_subscriptions s WHERE s.plan_id = p.id AND s.cancelled_at IS NULL) AS subscribers
            FROM subscription_plans p
            """;

    private BillingDTO.Plan mapPlan(ResultSet rs) throws SQLException {
        return new BillingDTO.Plan(rs.getString("id"), rs.getString("name"), rs.getString("description"),
                rs.getBigDecimal("price"), rs.getString("currency"), rs.getString("frequency"),
                (Integer) rs.getObject("max_students"), rs.getBoolean("active"), rs.getLong("subscribers"));
    }

    // ------------------------------------------------------------------ subscriptions

    @AuditLogAnnotation(action = "SUBSCRIPTION_CREATED")
    public BillingDTO.Detail createSubscription(BillingRequests.CreateSubscription p, String actorId) {
        var school = jdbc.queryForList("SELECT id, status FROM schools WHERE id = ?", p.schoolId());
        if (school.isEmpty() || "DELETED".equals(school.get(0).get("status"))) {
            throw new NotFoundException("School not found");
        }
        var planRows = jdbc.queryForList("SELECT id, price, frequency, active FROM subscription_plans WHERE id = ?", LeadService.parse(p.planId()));
        if (planRows.isEmpty()) {
            throw new NotFoundException("Plan not found");
        }
        var plan = planRows.get(0);
        if (!Boolean.TRUE.equals(plan.get("active"))) {
            throw new RuleException("That plan is switched off - choose an active plan");
        }
        Long live = jdbc.queryForObject("SELECT count(*) FROM school_subscriptions WHERE school_id = ? AND cancelled_at IS NULL", Long.class, p.schoolId());
        if (live != null && live > 0) {
            throw new AlreadyExistsException("This school already has a subscription - cancel it first to start another");
        }

        String frequency = (String) plan.get("frequency");
        BigDecimal price = scale(p.price() != null ? p.price() : (BigDecimal) plan.get("price"));
        var id = UUID.randomUUID();
        var now = java.sql.Timestamp.from(Instant.now());
        LocalDate end = periodEnd(p.startDate(), frequency);

        jdbc.update("""
                INSERT INTO school_subscriptions (id, school_id, plan_id, price, currency, frequency, start_date, current_period_end, notes, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, p.schoolId(), LeadService.parse(p.planId()), price, CURRENCY, frequency, p.startDate(), end, blank(p.notes()), actorId, now, now);
        issueInvoice(id, p.startDate(), end, price, actorId);
        return detail(id.toString());
    }

    @AuditLogAnnotation(action = "SUBSCRIPTION_UPDATED")
    public BillingDTO.Detail updateSubscription(String id, BillingRequests.UpdateSubscription p) {
        var uuid = LeadService.parse(id);
        int updated = jdbc.update("UPDATE school_subscriptions SET price = ?, notes = ?, updated_at = now() WHERE id = ? AND cancelled_at IS NULL",
                scale(p.price()), blank(p.notes()), uuid);
        if (updated == 0) {
            throw new RuleException("This subscription is cancelled or doesn't exist");
        }
        // The price on invoices already issued is not changed - it applies from the next one.
        return detail(id);
    }

    /** Invoices the next period. Called when a subscription is coming to the end of what has been invoiced. */
    @AuditLogAnnotation(action = "SUBSCRIPTION_RENEWED")
    public BillingDTO.Detail renew(String id, String actorId) {
        var uuid = LeadService.parse(id);
        var rows = jdbc.queryForList("SELECT price, frequency, current_period_end, cancelled_at FROM school_subscriptions WHERE id = ? FOR UPDATE", uuid);
        if (rows.isEmpty()) {
            throw new NotFoundException("Subscription not found");
        }
        var row = rows.get(0);
        if (row.get("cancelled_at") != null) {
            throw new RuleException("A cancelled subscription can't be renewed - start a new one");
        }
        LocalDate lastEnd = ((java.sql.Date) row.get("current_period_end")).toLocalDate();
        LocalDate today = LocalDate.now();
        // Only the next period, and only when the school is within the renewal window: invoicing years ahead helps nobody.
        int window = settings.intValue(PlatformSettingsService.RENEWAL_WINDOW_DAYS);
        if (lastEnd.isAfter(today.plusDays(window))) {
            throw new RuleException("This subscription is paid for until " + lastEnd + " - renew it within "
                    + window + " days of then");
        }
        LocalDate start = lastEnd.plusDays(1);
        LocalDate end = periodEnd(start, (String) row.get("frequency"));
        issueInvoice(uuid, start, end, (BigDecimal) row.get("price"), actorId);
        jdbc.update("UPDATE school_subscriptions SET current_period_end = ?, updated_at = now() WHERE id = ?", end, uuid);
        return detail(id);
    }

    /** Ends the subscription. Unpaid invoices are voided; the school itself is left exactly as it is. */
    @AuditLogAnnotation(action = "SUBSCRIPTION_CANCELLED")
    public BillingDTO.Detail cancel(String id, String reason) {
        var uuid = LeadService.parse(id);
        int updated = jdbc.update("UPDATE school_subscriptions SET cancelled_at = now(), cancel_reason = ?, updated_at = now() WHERE id = ? AND cancelled_at IS NULL",
                reason.trim(), uuid);
        if (updated == 0) {
            throw new RuleException("This subscription is already cancelled or doesn't exist");
        }
        // An invoice nobody has paid anything towards is no longer owed; one that has had a payment stays on record.
        jdbc.update("""
                UPDATE subscription_invoices i SET status = 'VOID', voided_at = now(), void_reason = 'Subscription cancelled'
                WHERE i.subscription_id = ? AND i.status = 'OPEN'
                  AND NOT EXISTS (SELECT 1 FROM subscription_payments p WHERE p.invoice_id = i.id AND p.voided_at IS NULL)
                """, uuid);
        return detail(id);
    }

    private void issueInvoice(UUID subscriptionId, LocalDate start, LocalDate end, BigDecimal amount, String actorId) {
        // How long a school has to pay, counted from the later of today and the period's start (a platform setting).
        LocalDate due = (start.isAfter(LocalDate.now()) ? start : LocalDate.now())
                .plusDays(settings.intValue(PlatformSettingsService.PAYMENT_TERMS_DAYS));
        String number = nextNumber("INV", "subscription_invoice_number_seq");
        jdbc.update("""
                INSERT INTO subscription_invoices (id, number, subscription_id, period_start, period_end, amount, due_date, status, issued_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN', now(), ?)
                """, UUID.randomUUID(), number, subscriptionId, start, end, scale(amount), due, actorId);
    }

    private String nextNumber(String prefix, String sequence) {
        // sequence is one of two fixed names above, never user input
        Long n = jdbc.queryForObject("SELECT nextval('" + sequence + "')", Long.class);
        return "%s-%d-%05d".formatted(prefix, LocalDate.now().getYear(), n);
    }

    // ------------------------------------------------------------------ invoices and payments

    @AuditLogAnnotation(action = "SUBSCRIPTION_PAYMENT_RECORDED")
    public BillingDTO.Detail recordPayment(String invoiceId, BillingRequests.RecordPayment p, String actorId) {
        var uuid = LeadService.parse(invoiceId);
        // Lock the invoice so two payments recorded at once can't together pay more than is owed.
        var rows = jdbc.queryForList("""
                SELECT i.id, i.subscription_id, i.status, i.amount,
                       i.amount - COALESCE((SELECT sum(x.amount) FROM subscription_payments x WHERE x.invoice_id = i.id AND x.voided_at IS NULL), 0) AS outstanding
                FROM subscription_invoices i WHERE i.id = ? FOR UPDATE
                """, uuid);
        if (rows.isEmpty()) {
            throw new NotFoundException("Invoice not found");
        }
        var inv = rows.get(0);
        if (!"OPEN".equals(inv.get("status"))) {
            throw new RuleException("This invoice has been voided");
        }
        BigDecimal amount = scale(p.amount());
        BigDecimal outstanding = (BigDecimal) inv.get("outstanding");
        if (amount.compareTo(outstanding) > 0) {
            throw new RuleException("That is more than the " + outstanding.toPlainString() + " still owed on this invoice");
        }
        boolean cash = "CASH".equals(p.method());
        if (!cash && blank(p.reference()) == null) {
            throw new RuleException("Enter the payment's reference (transaction or receipt number) so it can be checked");
        }
        Instant paidAt = p.paidAt() == null ? Instant.now() : p.paidAt();
        if (paidAt.isAfter(Instant.now().plusSeconds(60))) {
            throw new RuleException("A payment can't be dated in the future");
        }

        jdbc.update("""
                INSERT INTO subscription_payments (id, receipt_number, invoice_id, amount, method, reference, paid_at, note, recorded_by, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """, UUID.randomUUID(), nextNumber("RCT", "subscription_receipt_number_seq"), uuid, amount, p.method(),
                blank(p.reference()), java.sql.Timestamp.from(paidAt), blank(p.note()), actorId);
        return detail(inv.get("subscription_id").toString());
    }

    @AuditLogAnnotation(action = "SUBSCRIPTION_PAYMENT_VOIDED")
    public BillingDTO.Detail voidPayment(String paymentId, String reason, String actorId) {
        var uuid = LeadService.parse(paymentId);
        var rows = jdbc.queryForList("""
                SELECT i.subscription_id FROM subscription_payments p JOIN subscription_invoices i ON i.id = p.invoice_id
                WHERE p.id = ? AND p.voided_at IS NULL
                """, uuid);
        if (rows.isEmpty()) {
            throw new RuleException("This payment is already voided or doesn't exist");
        }
        jdbc.update("UPDATE subscription_payments SET voided_at = now(), voided_by = ?, void_reason = ? WHERE id = ?",
                actorId, reason.trim(), uuid);
        return detail(rows.get(0).get("subscription_id").toString());
    }

    @AuditLogAnnotation(action = "SUBSCRIPTION_INVOICE_VOIDED")
    public BillingDTO.Detail voidInvoice(String invoiceId, String reason) {
        var uuid = LeadService.parse(invoiceId);
        var rows = jdbc.queryForList("""
                SELECT i.subscription_id, i.status,
                       (SELECT count(*) FROM subscription_payments p WHERE p.invoice_id = i.id AND p.voided_at IS NULL) AS payments
                FROM subscription_invoices i WHERE i.id = ?
                """, uuid);
        if (rows.isEmpty()) {
            throw new NotFoundException("Invoice not found");
        }
        var row = rows.get(0);
        if (!"OPEN".equals(row.get("status"))) {
            throw new RuleException("This invoice is already voided");
        }
        if (((Number) row.get("payments")).longValue() > 0) {
            throw new RuleException("Payments have been recorded against this invoice - void them first");
        }
        jdbc.update("UPDATE subscription_invoices SET status = 'VOID', voided_at = now(), void_reason = ? WHERE id = ?", reason.trim(), uuid);
        return detail(row.get("subscription_id").toString());
    }

    // ------------------------------------------------------------------ reads

    private static final String SUBSCRIPTION_SELECT = """
            WITH inv AS (
                SELECT i.*, i.amount - COALESCE((SELECT sum(p.amount) FROM subscription_payments p
                                                  WHERE p.invoice_id = i.id AND p.voided_at IS NULL), 0) AS outstanding
                FROM subscription_invoices i WHERE i.status = 'OPEN'
            )
            SELECT s.*, sc.name AS school_name, sc.domain AS school_domain, pl.name AS plan_name,
                   COALESCE((SELECT sum(outstanding) FROM inv WHERE inv.subscription_id = s.id), 0) AS outstanding,
                   COALESCE((SELECT sum(outstanding) FROM inv WHERE inv.subscription_id = s.id
                              AND inv.outstanding > 0 AND inv.due_date < current_date), 0) AS overdue_amount,
                   EXISTS (SELECT 1 FROM inv WHERE inv.subscription_id = s.id AND inv.outstanding <= 0
                            AND current_date BETWEEN inv.period_start AND inv.period_end) AS covered,
                   (SELECT max(period_end) FROM inv WHERE inv.subscription_id = s.id AND inv.outstanding <= 0) AS paid_through
            FROM school_subscriptions s
            JOIN schools sc ON sc.id = s.school_id
            JOIN subscription_plans pl ON pl.id = s.plan_id
            """;

    public record Filter(String state, String planId, String search) {
    }

    /** Subscriptions, newest first. The state is worked out per row, so filtering by it happens after the query. */
    public Page<BillingDTO.Subscription> subscriptions(Filter f, int page, int size) {
        var all = allSubscriptions(f.planId(), f.search()).stream()
                .filter(s -> f.state() == null || f.state().isBlank() || s.state().equals(f.state()))
                .toList();
        int from = Math.min((Math.max(page, 1) - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(Math.max(page, 1) - 1, size), all.size());
    }

    private List<BillingDTO.Subscription> allSubscriptions(String planId, String search) {
        var where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();
        if (planId != null && !planId.isBlank()) {
            where.append(" AND s.plan_id = ?");
            args.add(LeadService.parse(planId));
        }
        if (search != null && !search.isBlank()) {
            where.append(" AND (lower(sc.name) LIKE ? OR lower(sc.domain) LIKE ?)");
            String like = "%" + search.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
        }
        return jdbc.query(SUBSCRIPTION_SELECT + where + " ORDER BY s.created_at DESC", (rs, i) -> mapSubscription(rs), args.toArray());
    }

    public BillingDTO.Detail detail(String id) {
        var uuid = LeadService.parse(id);
        var rows = jdbc.query(SUBSCRIPTION_SELECT + " WHERE s.id = ?", (rs, i) -> mapSubscription(rs), uuid);
        if (rows.isEmpty()) {
            throw new NotFoundException("Subscription not found");
        }
        return new BillingDTO.Detail(rows.get(0), invoicesOf(uuid));
    }

    /** A school's subscription history, newest first (a school can have had several, one at a time). */
    public List<BillingDTO.Detail> forSchool(String schoolId) {
        var ids = jdbc.queryForList("SELECT id FROM school_subscriptions WHERE school_id = ? ORDER BY created_at DESC", String.class, schoolId);
        return ids.stream().map(this::detail).toList();
    }

    private List<BillingDTO.Invoice> invoicesOf(UUID subscriptionId) {
        var payments = jdbc.query(PAYMENT_SELECT + " WHERE i.subscription_id = ? ORDER BY p.paid_at DESC",
                (rs, i) -> mapPayment(rs), subscriptionId).stream()
                .collect(Collectors.groupingBy(BillingDTO.Payment::invoiceId));
        return jdbc.query("""
                SELECT i.*, sc.name AS school_name,
                       COALESCE((SELECT sum(p.amount) FROM subscription_payments p WHERE p.invoice_id = i.id AND p.voided_at IS NULL), 0) AS paid
                FROM subscription_invoices i
                JOIN school_subscriptions s ON s.id = i.subscription_id
                JOIN schools sc ON sc.id = s.school_id
                WHERE i.subscription_id = ? ORDER BY i.period_start DESC
                """, (rs, i) -> {
                    var amount = rs.getBigDecimal("amount");
                    var paid = rs.getBigDecimal("paid");
                    var outstanding = "VOID".equals(rs.getString("status")) ? BigDecimal.ZERO : amount.subtract(paid);
                    var due = rs.getDate("due_date").toLocalDate();
                    return new BillingDTO.Invoice(rs.getString("id"), rs.getString("number"),
                            rs.getString("subscription_id"), rs.getString("school_name"),
                            rs.getDate("period_start").toLocalDate(), rs.getDate("period_end").toLocalDate(), amount,
                            paid, outstanding, due, rs.getString("status"),
                            "OPEN".equals(rs.getString("status")) && outstanding.signum() > 0 && due.isBefore(LocalDate.now()),
                            instant(rs, "issued_at"), instant(rs, "voided_at"), rs.getString("void_reason"),
                            payments.getOrDefault(rs.getString("id"), List.of()));
                }, subscriptionId);
    }

    private static final String PAYMENT_SELECT = """
            SELECT p.*, i.number AS invoice_number, sc.name AS school_name,
                   trim(concat_ws(' ', u.given_name, u.family_name)) AS recorder
            FROM subscription_payments p
            JOIN subscription_invoices i ON i.id = p.invoice_id
            JOIN school_subscriptions s ON s.id = i.subscription_id
            JOIN schools sc ON sc.id = s.school_id
            LEFT JOIN users u ON u.id = p.recorded_by
            """;

    /** Every payment recorded, newest first - the platform's transaction history. */
    public Page<BillingDTO.Payment> payments(String search, int page, int size) {
        var where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();
        if (search != null && !search.isBlank()) {
            where.append(" AND (lower(sc.name) LIKE ? OR lower(p.receipt_number) LIKE ? OR lower(i.number) LIKE ? OR lower(coalesce(p.reference,'')) LIKE ?)");
            String like = "%" + search.trim().toLowerCase() + "%";
            for (int k = 0; k < 4; k++) {
                args.add(like);
            }
        }
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM subscription_payments p JOIN subscription_invoices i ON i.id = p.invoice_id
                JOIN school_subscriptions s ON s.id = i.subscription_id JOIN schools sc ON sc.id = s.school_id
                """ + where, Long.class, args.toArray());
        var pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) Math.max(page - 1, 0) * size);
        var rows = jdbc.query(PAYMENT_SELECT + where + " ORDER BY p.paid_at DESC LIMIT ? OFFSET ?", (rs, i) -> mapPayment(rs), pageArgs.toArray());
        return new PageImpl<>(rows, PageRequest.of(Math.max(page, 1) - 1, size), total == null ? 0 : total);
    }

    /** Subscriptions whose invoiced period ends within the window and have no next period invoiced yet. */
    public List<BillingDTO.Renewal> renewals(int days) {
        LocalDate today = LocalDate.now();
        return allSubscriptions(null, null).stream()
                .filter(s -> s.cancelledAt() == null && !s.currentPeriodEnd().isAfter(today.plusDays(days)))
                .sorted((a, b) -> a.currentPeriodEnd().compareTo(b.currentPeriodEnd()))
                .map(s -> {
                    LocalDate nextStart = s.currentPeriodEnd().plusDays(1);
                    return new BillingDTO.Renewal(s, java.time.temporal.ChronoUnit.DAYS.between(today, s.currentPeriodEnd()),
                            nextStart, periodEnd(nextStart, s.frequency()));
                }).toList();
    }

    public BillingDTO.Summary summary(int days) {
        var subs = allSubscriptions(null, null);
        var byState = new LinkedHashMap<String, Long>();
        for (State s : State.values()) {
            byState.put(s.name(), 0L);
        }
        subs.forEach(s -> byState.merge(s.state(), 1L, Long::sum));

        BigDecimal outstanding = subs.stream().filter(s -> s.cancelledAt() == null).map(BillingDTO.Subscription::outstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overdue = subs.stream().filter(s -> s.cancelledAt() == null).map(BillingDTO.Subscription::overdueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueInvoices = count("""
                SELECT count(*) FROM (
                    SELECT i.id FROM subscription_invoices i JOIN school_subscriptions s ON s.id = i.subscription_id
                    WHERE i.status = 'OPEN' AND s.cancelled_at IS NULL AND i.due_date < current_date
                      AND i.amount > COALESCE((SELECT sum(p.amount) FROM subscription_payments p WHERE p.invoice_id = i.id AND p.voided_at IS NULL), 0)
                ) x""");
        long expiring = renewals(settings.intValue(PlatformSettingsService.RENEWAL_WINDOW_DAYS)).stream().filter(r -> r.daysLeft() >= 0).count();

        return new BillingDTO.Summary(CURRENCY, count("SELECT count(*) FROM subscription_plans WHERE active"),
                byState.get("ACTIVE"), byState,
                money("SELECT COALESCE(sum(amount), 0) FROM subscription_payments WHERE voided_at IS NULL AND paid_at >= now() - make_interval(days => ?)", days),
                money("SELECT COALESCE(sum(amount), 0) FROM subscription_payments WHERE voided_at IS NULL AND paid_at >= date_trunc('month', now())"),
                outstanding, overdue, overdueInvoices, expiring);
    }

    /** Whether any billing has been set up - the dashboard only shows revenue once there is something to show. */
    public boolean configured() {
        return count("SELECT (SELECT count(*) FROM subscription_plans) + (SELECT count(*) FROM school_subscriptions)") > 0;
    }

    // ------------------------------------------------------------------ mapping

    private BillingDTO.Subscription mapSubscription(ResultSet rs) throws SQLException {
        boolean cancelled = rs.getTimestamp("cancelled_at") != null;
        LocalDate end = rs.getDate("current_period_end").toLocalDate();
        BigDecimal overdue = rs.getBigDecimal("overdue_amount");
        var state = stateOf(cancelled, overdue.signum() > 0, rs.getBoolean("covered"), end, LocalDate.now());
        var paidThrough = rs.getDate("paid_through");
        return new BillingDTO.Subscription(rs.getString("id"), rs.getString("school_id"), rs.getString("school_name"),
                rs.getString("school_domain"), rs.getString("plan_id"), rs.getString("plan_name"),
                rs.getBigDecimal("price"), rs.getString("currency"), rs.getString("frequency"),
                rs.getDate("start_date").toLocalDate(), end, state.name(), rs.getBigDecimal("outstanding"), overdue,
                paidThrough == null ? null : paidThrough.toLocalDate(), instant(rs, "cancelled_at"),
                rs.getString("cancel_reason"), rs.getString("notes"), instant(rs, "created_at"));
    }

    private BillingDTO.Payment mapPayment(ResultSet rs) throws SQLException {
        return new BillingDTO.Payment(rs.getString("id"), rs.getString("receipt_number"), rs.getString("invoice_id"),
                rs.getString("invoice_number"), rs.getString("school_name"), rs.getBigDecimal("amount"),
                rs.getString("method"), rs.getString("reference"), instant(rs, "paid_at"), rs.getString("note"),
                blank(rs.getString("recorder")), instant(rs, "voided_at"), rs.getString("void_reason"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var t = rs.getTimestamp(column);
        return t == null ? null : Instant.ofEpochMilli(t.getTime());
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    private BigDecimal money(String sql, Object... args) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
