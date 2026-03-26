package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.OutstandingBalanceDTO;
import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.usecase.FinanceReportUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/report/finance")
@RequiredArgsConstructor
public class FinanceReportController {
    private final FinanceReportUseCase reportUseCase;

    @GetMapping("/total")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<Object> totalCollected(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var total = reportUseCase.totalCollected(school);
        return new ApiResponse<>("success", 200, "Total fees collected fetched successfully", total);
    }

    @GetMapping("/outstanding")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'PARENT', 'ACCOUNTANT')")
    public ApiResponse<List<OutstandingBalanceDTO>> outstanding(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) String studentId) {
        var res = reportUseCase.outstandingForStudent(school, studentId);
        return new ApiResponse<>("success", 200, "Outstanding balances fetched successfully",
                res);
    }

    @GetMapping("/outstanding/list")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'PARENT', 'ACCOUNTANT')")
    public ApiResponse<List<OutstandingBalanceDTO>> outstandingList(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) String studentId) {
        var res = reportUseCase.outstandingOnlyForStudent(school, studentId);
        return new ApiResponse<>("success", 200, "Outstanding balances fetched successfully",
                res);
    }

    @GetMapping("/payments")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'PARENT', 'ACCOUNTANT')")
    public ApiResponse<List<PaymentDTO>> paymentHistory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true) String studentId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = reportUseCase.paymentHistory(school, studentId, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Payment history fetched successfully", list, meta);
    }
}
