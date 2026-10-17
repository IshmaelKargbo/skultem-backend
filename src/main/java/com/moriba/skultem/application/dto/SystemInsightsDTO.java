package com.moriba.skultem.application.dto;

import java.util.List;

// What the system admin's dashboard reads beyond the three headline totals.
public record SystemInsightsDTO(
        int days,
        Schools schools,
        Growth growth,
        List<MonthPoint> monthly,
        List<SchoolRow> topSchools,
        List<SchoolRow> needsAttention,
        Demos demos,
        Content content,
        List<RecentSchool> recentSchools,
        List<ActivityRow> recentActivity,
        Leads leads,
        Onboarding onboarding,
        // Null until a plan or subscription exists - there is nothing to report before that.
        Billing billing,
        java.util.List<BillingDTO.Renewal> upcomingRenewals,
        // Null for an admin whose role has no support access.
        Support support) {

    // Live = active and not a playground school; playground = still being set up or tried out.
    public record Schools(long total, long live, long playground, long inactive) {
    }

    // The chosen period (days) against the period of the same length before it.
    public record Growth(Delta schools, Delta students, Delta users, Delta demoRequests) {
    }

    public record Delta(long current, long previous) {
    }

    public record MonthPoint(String month, long schools, long students, long demoRequests) {
    }

    // students is the active ones; reason is only set for the needs-attention list.
    public record SchoolRow(String id, String name, String domain, long students, boolean playground, String reason,
            java.time.Instant createdAt) {
    }

    public record Demos(long total, long inPeriod) {
    }

    public record Content(long unreadMessages, long publishedPosts, long draftPosts, long publishedTestimonials) {
    }

    // A school that recently registered.
    public record RecentSchool(String id, String name, String domain, String status, boolean playground,
            String ownerName, java.time.Instant createdAt) {
    }

    // One recorded action on the platform, from the audit log. schoolName / userName are null for platform-level
    // actions or when the record no longer exists.
    public record ActivityRow(String id, String action, String status, String schoolName, String userName,
            java.time.Instant createdAt) {
    }

    // The sales pipeline: leads still being worked, how many are waiting on a follow-up, and how many nobody owns.
    public record Leads(long open, long awaitingFollowUp, long unassigned, long overdue) {
    }

    // Schools being brought onto the platform.
    public record Onboarding(long inProgress, long completed, long overdueSteps) {
    }

    // What schools pay Skultem.
    public record Billing(String currency, long activeSubscriptions, java.math.BigDecimal revenueInPeriod,
            java.math.BigDecimal outstanding, java.math.BigDecimal overdueAmount, long overdueInvoices, long expiringSoon) {
    }

    // Support tickets that still need work.
    public record Support(long open, long unassigned, long urgent) {
    }
}
