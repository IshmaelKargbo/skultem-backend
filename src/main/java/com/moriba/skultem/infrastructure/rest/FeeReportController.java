package com.moriba.skultem.infrastructure.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.DailyCollectionReportDTO;
import com.moriba.skultem.application.dto.FeeDashboardSummaryDTO;
import com.moriba.skultem.application.dto.FeePaymentRowDTO;
import com.moriba.skultem.application.dto.FeeTermSummaryDTO;
import com.moriba.skultem.application.dto.OutstandingFeesReportDTO;
import com.moriba.skultem.application.dto.PaymentMethodSummaryDTO;
import com.moriba.skultem.application.dto.StudentFeeBalanceDTO;
import com.moriba.skultem.application.usecase.GenerateFeeTermSummaryUseCase;
import com.moriba.skultem.application.usecase.GetDailyCollectionReportUseCase;
import com.moriba.skultem.application.usecase.GetFeeDashboardSummaryUseCase;
import com.moriba.skultem.application.usecase.GetOutstandingFeesReportUseCase;
import com.moriba.skultem.application.usecase.GetPaymentHistoryReportUseCase;
import com.moriba.skultem.application.usecase.GetPaymentMethodSummaryUseCase;
import com.moriba.skultem.application.usecase.GetStudentFeeBalancesUseCase;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * Management reporting for the school's own fees - dashboard, term summary, student balances,
 * outstanding fees, daily collection, payment history and payment method breakdown. The platform
 * fee is never included in any of these (see LoadSchoolFeeRowsUseCase / PaymentJpaRepository's
 * school-fee-only queries) - it has its own reporting under /fee/platform/*.
 *
 * Same role gating as the rest of the fee-reporting endpoints (/fee/ledger/report,
 * /fee/platform/report): OWNER, PROPRIETOR, ACCOUNTANT - not the attendance-report convention
 * (ADMIN, OWNER, PROPRIETOR), since this is financial data and accountants need it.
 */
@RestController
@RequestMapping("/api/v1/fee/report")
@RequiredArgsConstructor
public class FeeReportController {

    private static final String REPORT_ROLES = "@permissionService.hasAnySchoolRole(#school, 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')";

    private final GetFeeDashboardSummaryUseCase getFeeDashboardSummaryUseCase;
    private final GenerateFeeTermSummaryUseCase generateFeeTermSummaryUseCase;
    private final GetStudentFeeBalancesUseCase getStudentFeeBalancesUseCase;
    private final GetOutstandingFeesReportUseCase getOutstandingFeesReportUseCase;
    private final GetDailyCollectionReportUseCase getDailyCollectionReportUseCase;
    private final GetPaymentHistoryReportUseCase getPaymentHistoryReportUseCase;
    private final GetPaymentMethodSummaryUseCase getPaymentMethodSummaryUseCase;

    @GetMapping("/dashboard")
    @PreAuthorize(REPORT_ROLES)
    public ApiResponse<FeeDashboardSummaryDTO> dashboard(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId) {
        var res = getFeeDashboardSummaryUseCase.execute(school, academicYearId, termId);
        return new ApiResponse<>("success", 200, "Fee dashboard summary fetched successfully", res);
    }

    @GetMapping("/term-summary")
    @PreAuthorize(REPORT_ROLES)
    public ApiResponse<FeeTermSummaryDTO> termSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String classSessionId,
            @RequestParam(required = false) String feeCategoryId) {
        var res = generateFeeTermSummaryUseCase.execute(school, academicYearId, termId, classSessionId,
                feeCategoryId);
        return new ApiResponse<>("success", 200, "Fee term summary fetched successfully", res);
    }

    @GetMapping("/student-balances")
    @PreAuthorize(REPORT_ROLES)
    public ApiResponse<java.util.List<StudentFeeBalanceDTO>> studentBalances(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String classSessionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feeCategoryId,
            @RequestParam(required = false) BigDecimal balanceMin,
            @RequestParam(required = false) BigDecimal balanceMax,
            @RequestParam(required = false, defaultValue = "NAME") GetStudentFeeBalancesUseCase.SortBy sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = getStudentFeeBalancesUseCase.execute(school, academicYearId, termId, classSessionId, status,
                feeCategoryId, balanceMin, balanceMax, sortBy, "asc".equalsIgnoreCase(direction), page - 1, size);
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Student fee balances fetched successfully", res.getContent(),
                meta);
    }

    @GetMapping("/outstanding")
    @PreAuthorize(REPORT_ROLES)
    public ApiResponse<OutstandingFeesReportDTO> outstanding(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId) {
        var res = getOutstandingFeesReportUseCase.execute(school, academicYearId, termId);
        return new ApiResponse<>("success", 200, "Outstanding fees report fetched successfully", res);
    }

    @GetMapping("/daily-collection")
    @PreAuthorize(REPORT_ROLES)
    public ApiResponse<DailyCollectionReportDTO> dailyCollection(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var res = getDailyCollectionReportUseCase.execute(school, from, to);
        return new ApiResponse<>("success", 200, "Daily collection report fetched successfully", res);
    }

    @GetMapping("/payment-history")
    @PreAuthorize(REPORT_ROLES)
    public ApiResponse<java.util.List<FeePaymentRowDTO>> paymentHistory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String classSessionId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String recordedByUserId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var zone = java.time.ZoneId.systemDefault();
        var start = from != null ? from.atStartOfDay(zone).toInstant() : null;
        var end = to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : null;
        PaymentMethod parsedMethod = (method != null && !method.isBlank()) ? PaymentMethod.valueOf(method) : null;
        var res = getPaymentHistoryReportUseCase.execute(school, start, end, academicYearId, termId, classSessionId,
                studentId, parsedMethod, recordedByUserId, page - 1, size);
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Payment history fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/payment-methods")
    @PreAuthorize(REPORT_ROLES)
    public ApiResponse<PaymentMethodSummaryDTO> paymentMethods(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var res = getPaymentMethodSummaryUseCase.execute(school, academicYearId, termId, from, to);
        return new ApiResponse<>("success", 200, "Payment method summary fetched successfully", res);
    }
}
