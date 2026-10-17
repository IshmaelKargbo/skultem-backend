package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// What a school pays Skultem - plans, subscriptions, invoices and the payments recorded against them. Nothing
// here has anything to do with the fees a school collects from its parents.
public final class BillingDTO {

    private BillingDTO() {
    }

    public record Plan(String id, String name, String description, BigDecimal price, String currency, String frequency,
            Integer maxStudents, boolean active, long subscribers) {
    }

    public record Subscription(String id, String schoolId, String schoolName, String schoolDomain, String planId,
            String planName, BigDecimal price, String currency, String frequency, LocalDate startDate,
            LocalDate currentPeriodEnd,
            // ACTIVE, PENDING, OVERDUE, EXPIRED or CANCELLED - worked out from the invoices and today's date.
            String state,
            BigDecimal outstanding, BigDecimal overdueAmount,
            // Up to when the school has paid (the end of its latest fully paid period).
            LocalDate paidThrough, Instant cancelledAt, String cancelReason, String notes, Instant createdAt) {
    }

    public record Payment(String id, String receiptNumber, String invoiceId, String invoiceNumber, String schoolName,
            BigDecimal amount, String method, String reference, Instant paidAt, String note, String recordedByName,
            Instant voidedAt, String voidReason) {
    }

    public record Invoice(String id, String number, String subscriptionId, String schoolName, LocalDate periodStart,
            LocalDate periodEnd, BigDecimal amount, BigDecimal paid, BigDecimal outstanding, LocalDate dueDate,
            String status, boolean overdue, Instant issuedAt, Instant voidedAt, String voidReason,
            List<Payment> payments) {
    }

    public record Detail(Subscription subscription, List<Invoice> invoices) {
    }

    // A subscription whose paid-for period is about to run out, and the period the next invoice would cover.
    public record Renewal(Subscription subscription, long daysLeft, LocalDate nextPeriodStart,
            LocalDate nextPeriodEnd) {
    }

    public record Summary(String currency, long plans, long activeSubscriptions, Map<String, Long> byState,
            BigDecimal revenueInPeriod, BigDecimal revenueThisMonth, BigDecimal outstanding,
            BigDecimal overdueAmount, long overdueInvoices, long expiringSoon) {
    }
}
