package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.BillingDTO;
import com.moriba.skultem.application.services.BillingService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.BillingRequests;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// What schools pay Skultem: plans, subscriptions, invoices and the payments recorded for them. Platform-level, so
// system admins only - and entirely separate from the fees a school collects from its own parents.
@RestController
@RequestMapping("/api/v1/system/billing")
@PreAuthorize("@permissionService.hasPlatformPermission('BILLING')")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billing;

    private static Map<String, Object> meta(Page<?> res) {
        return Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
    }

    @GetMapping("/summary")
    public ApiResponse<BillingDTO.Summary> summary(@RequestParam(defaultValue = "30") int days) {
        return new ApiResponse<>("success", 200, "Billing summary fetched successfully", billing.summary(days));
    }

    // ---- Plans ----

    @GetMapping("/plans")
    public ApiResponse<List<BillingDTO.Plan>> plans() {
        return new ApiResponse<>("success", 200, "Plans fetched successfully", billing.plans());
    }

    @PostMapping("/plans")
    public ApiResponse<BillingDTO.Plan> createPlan(@Valid @RequestBody BillingRequests.SavePlan param) {
        return new ApiResponse<>("success", 200, "Plan created successfully", billing.savePlan(null, param));
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<BillingDTO.Plan> updatePlan(@PathVariable String id, @Valid @RequestBody BillingRequests.SavePlan param) {
        return new ApiResponse<>("success", 200, "Plan updated successfully", billing.savePlan(id, param));
    }

    // ---- Subscriptions ----

    @GetMapping("/subscriptions")
    public ApiResponse<List<BillingDTO.Subscription>> subscriptions(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String planId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = billing.subscriptions(new BillingService.Filter(state, planId, search), page, Math.min(size, 100));
        return new ApiResponse<>("success", 200, "Subscriptions fetched successfully", res.getContent(), meta(res));
    }

    @GetMapping("/subscriptions/{id}")
    public ApiResponse<BillingDTO.Detail> subscription(@PathVariable String id) {
        return new ApiResponse<>("success", 200, "Subscription fetched successfully", billing.detail(id));
    }

    @GetMapping("/schools/{schoolId}")
    public ApiResponse<List<BillingDTO.Detail>> forSchool(@PathVariable String schoolId) {
        return new ApiResponse<>("success", 200, "Subscription history fetched successfully", billing.forSchool(schoolId));
    }

    @PostMapping("/subscriptions")
    public ApiResponse<BillingDTO.Detail> createSubscription(@AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody BillingRequests.CreateSubscription param) {
        return new ApiResponse<>("success", 200, "Subscription created successfully", billing.createSubscription(param, userId));
    }

    @PutMapping("/subscriptions/{id}")
    public ApiResponse<BillingDTO.Detail> updateSubscription(@PathVariable String id,
            @Valid @RequestBody BillingRequests.UpdateSubscription param) {
        return new ApiResponse<>("success", 200, "Subscription updated successfully", billing.updateSubscription(id, param));
    }

    @PostMapping("/subscriptions/{id}/renew")
    public ApiResponse<BillingDTO.Detail> renew(@AuthenticationPrincipal(expression = "userId") String userId, @PathVariable String id) {
        return new ApiResponse<>("success", 200, "Renewal invoiced successfully", billing.renew(id, userId));
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public ApiResponse<BillingDTO.Detail> cancel(@PathVariable String id, @Valid @RequestBody BillingRequests.Reason param) {
        return new ApiResponse<>("success", 200, "Subscription cancelled successfully", billing.cancel(id, param.reason()));
    }

    @GetMapping("/renewals")
    public ApiResponse<List<BillingDTO.Renewal>> renewals(@RequestParam(defaultValue = "30") int days) {
        return new ApiResponse<>("success", 200, "Renewals fetched successfully", billing.renewals(Math.min(Math.max(days, 1), 365)));
    }

    // ---- Invoices and payments ----

    @PostMapping("/invoices/{id}/payments")
    public ApiResponse<BillingDTO.Detail> recordPayment(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody BillingRequests.RecordPayment param) {
        return new ApiResponse<>("success", 200, "Payment recorded successfully", billing.recordPayment(id, param, userId));
    }

    @PostMapping("/invoices/{id}/void")
    public ApiResponse<BillingDTO.Detail> voidInvoice(@PathVariable String id, @Valid @RequestBody BillingRequests.Reason param) {
        return new ApiResponse<>("success", 200, "Invoice voided successfully", billing.voidInvoice(id, param.reason()));
    }

    @GetMapping("/payments")
    public ApiResponse<List<BillingDTO.Payment>> payments(@RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = billing.payments(search, page, Math.min(size, 100));
        return new ApiResponse<>("success", 200, "Payments fetched successfully", res.getContent(), meta(res));
    }

    @PostMapping("/payments/{id}/void")
    public ApiResponse<BillingDTO.Detail> voidPayment(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody BillingRequests.Reason param) {
        return new ApiResponse<>("success", 200, "Payment voided successfully", billing.voidPayment(id, param.reason(), userId));
    }
}
