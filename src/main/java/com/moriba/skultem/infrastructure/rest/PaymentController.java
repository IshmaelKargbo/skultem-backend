package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.usecase.ListStudentPaymentBySchoolUseCase;
import com.moriba.skultem.application.usecase.RecordPaymentUseCase;
import com.moriba.skultem.application.usecase.RecordPaymentUseCase.PaymentRecord;
import com.moriba.skultem.application.usecase.SumStudentPaymentByFeeThisYearUseCase;
import com.moriba.skultem.application.usecase.SumStudentPaymentByFeeUseCase;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.RecordPaymentDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final RecordPaymentUseCase recordPaymentUseCase;
    private final SumStudentPaymentByFeeUseCase sumStudentPaymentByFeeUseCase;
    private final SumStudentPaymentByFeeThisYearUseCase studentPaymentByFeeThisYearUseCase;
    private final ListStudentPaymentBySchoolUseCase listStudentPaymentBySchoolUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<PaymentDTO> record(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody RecordPaymentDTO param) {
        var method = PaymentMethod.valueOf(param.method());
        var payload = new PaymentRecord(school, param.studentId(), param.feeId(), param.amount(), method,
                param.referenceNo(), param.note());
        var res = recordPaymentUseCase.execute(payload);
        return new ApiResponse<>("success", 200, "Payment recorded successfully", res);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<BigDecimal> countThisYearFees(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId) {
        var res = studentPaymentByFeeThisYearUseCase.execute(school, studentId);
        return new ApiResponse<>("success", 200, "Student payment sum for this year successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<PaymentDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listStudentPaymentBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Payments fetched successfully", list, meta);
    }

    @GetMapping("/student/{studentId}/{feeId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<BigDecimal> sumByStudentAndFee(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String feeId, @PathVariable String studentId) {
        var res = sumStudentPaymentByFeeUseCase.execute(feeId, studentId);
        return new ApiResponse<>("success", 200, "Student payment sum successfully", res);
    }
}
