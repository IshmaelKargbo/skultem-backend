package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.MaterialSaleDTO;
import com.moriba.skultem.application.dto.MaterialSaleSummaryDTO;
import com.moriba.skultem.application.services.MaterialSaleService;
import com.moriba.skultem.domain.model.MaterialSale.PaymentMethod;
import com.moriba.skultem.domain.model.MaterialSale.Status;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateMaterialSaleDTO;
import com.moriba.skultem.infrastructure.rest.dto.FulfillMaterialSaleDTO;
import com.moriba.skultem.infrastructure.rest.dto.RecordSalePaymentDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Point-of-sale style transactions for school materials (uniforms, socks, textbooks, ...) - a
// cash sale to a student or a walk-in buyer, independent of the fee-entitled Supply flow at
// /materials/supply. Nested under /materials since a sale always references a material, matching
// how /materials/supply and /materials/category are organized.
@RestController
@RequestMapping("/api/v1/materials/sales")
@RequiredArgsConstructor
public class MaterialSaleController {

    private final MaterialSaleService service;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialSaleDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateMaterialSaleDTO param) {
        var res = service.create(
                school,
                param.materialId(),
                param.studentId(),
                param.customerName(),
                param.quantity(),
                param.unitPrice(),
                param.amountPaid(),
                param.paymentMethod() != null ? PaymentMethod.valueOf(param.paymentMethod()) : null,
                param.note(),
                param.collectNow());

        return new ApiResponse<>("success", 200, "Sale recorded successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<MaterialSaleDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean paidPending) {

        Status parsedStatus = status != null && !status.isBlank() ? Status.valueOf(status) : null;
        var res = service.list(school, page, size, search, parsedStatus, paidPending);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Sales fetched successfully", list, meta);
    }

    @GetMapping("/summary")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialSaleSummaryDTO> summary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = service.summary(school);
        return new ApiResponse<>("success", 200, "Sales summary fetched successfully", res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<MaterialSaleDTO> findOne(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = service.findOne(school, id);
        return new ApiResponse<>("success", 200, "Sale fetched successfully", res);
    }

    @PostMapping("/{id}/fulfill")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialSaleDTO> fulfill(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody(required = false) FulfillMaterialSaleDTO param) {
        var res = service.fulfill(school, id, param != null ? param.note() : null);
        return new ApiResponse<>("success", 200, "Sale fulfilled successfully", res);
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialSaleDTO> recordPayment(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody RecordSalePaymentDTO param) {
        var res = service.recordPayment(
                school,
                id,
                param.amount(),
                param.paymentMethod() != null ? PaymentMethod.valueOf(param.paymentMethod()) : null);

        return new ApiResponse<>("success", 200, "Payment recorded successfully", res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialSaleDTO> cancel(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = service.cancel(school, id);
        return new ApiResponse<>("success", 200, "Sale cancelled successfully", res);
    }
}
