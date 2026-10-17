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

import com.moriba.skultem.application.dto.LeadDTO;
import com.moriba.skultem.application.dto.OnboardingDTO;
import com.moriba.skultem.application.services.LeadService;
import com.moriba.skultem.application.services.OnboardingService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.LeadActionDTOs;
import com.moriba.skultem.infrastructure.rest.dto.OnboardingActionDTOs;
import com.moriba.skultem.infrastructure.rest.dto.SaveLeadDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// The system admin's sales pipeline: leads (from demo requests, contact messages or added by hand) and the
// onboarding of the ones that become schools. Platform-level, so system admins only.
@RestController
@RequestMapping("/api/v1/system")
@PreAuthorize("@permissionService.hasPlatformPermission('MARKETING')")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final OnboardingService onboardingService;

    private static Map<String, Object> meta(Page<?> res) {
        return Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
    }

    // ---- Leads ----

    @GetMapping("/leads")
    public ApiResponse<List<LeadDTO>> leads(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String view,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = leadService.list(new LeadService.Filter(status, assignedTo, search, view), page, Math.min(size, 100));
        return new ApiResponse<>("success", 200, "Leads fetched successfully", res.getContent(), meta(res));
    }

    @GetMapping("/leads/summary")
    public ApiResponse<LeadDTO.Summary> summary() {
        return new ApiResponse<>("success", 200, "Lead summary fetched successfully", leadService.summary());
    }

    @GetMapping("/staff")
    public ApiResponse<List<LeadDTO.Staff>> staff() {
        return new ApiResponse<>("success", 200, "Staff fetched successfully", leadService.staff());
    }

    @GetMapping("/leads/{id}")
    public ApiResponse<LeadDTO.Detail> lead(@PathVariable String id) {
        return new ApiResponse<>("success", 200, "Lead fetched successfully", leadService.get(id));
    }

    @PostMapping("/leads")
    public ApiResponse<LeadDTO> create(@AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody SaveLeadDTO param) {
        return new ApiResponse<>("success", 200, "Lead created successfully", leadService.create(param, userId));
    }

    @PutMapping("/leads/{id}")
    public ApiResponse<LeadDTO> update(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody SaveLeadDTO param) {
        return new ApiResponse<>("success", 200, "Lead updated successfully", leadService.update(id, param, userId));
    }

    @PostMapping("/leads/{id}/status")
    public ApiResponse<LeadDTO> status(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody LeadActionDTOs.ChangeStatus param) {
        return new ApiResponse<>("success", 200, "Lead status updated successfully",
                leadService.changeStatus(id, param.status(), param.reason(), userId));
    }

    @PostMapping("/leads/{id}/demo")
    public ApiResponse<LeadDTO> demo(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody LeadActionDTOs.Demo param) {
        return new ApiResponse<>("success", 200, "Demo updated successfully",
                leadService.demo(id, param.status(), param.at(), userId));
    }

    @PostMapping("/leads/{id}/notes")
    public ApiResponse<LeadDTO> note(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody LeadActionDTOs.AddNote param) {
        return new ApiResponse<>("success", 200, "Note added successfully",
                leadService.addNote(id, param.kind(), param.body(), userId));
    }

    @PostMapping("/leads/{id}/convert")
    public ApiResponse<LeadDTO> convert(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody LeadActionDTOs.Convert param) {
        return new ApiResponse<>("success", 200, "Lead converted successfully",
                leadService.convert(id, param.schoolId(), userId));
    }

    @PostMapping("/leads/{id}/onboarding")
    public ApiResponse<OnboardingDTO> startOnboarding(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id) {
        return new ApiResponse<>("success", 200, "Onboarding started successfully",
                onboardingService.start(id, userId));
    }

    // ---- Onboarding ----

    @GetMapping("/onboardings")
    public ApiResponse<List<OnboardingDTO>> onboardings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = onboardingService.list(status, search, page, Math.min(size, 100));
        return new ApiResponse<>("success", 200, "Onboardings fetched successfully", res.getContent(), meta(res));
    }

    @GetMapping("/onboardings/{id}")
    public ApiResponse<OnboardingDTO> onboarding(@PathVariable String id) {
        return new ApiResponse<>("success", 200, "Onboarding fetched successfully", onboardingService.get(id));
    }

    @PutMapping("/onboardings/{id}")
    public ApiResponse<OnboardingDTO> updateOnboarding(@PathVariable String id,
            @Valid @RequestBody OnboardingActionDTOs.Update param) {
        return new ApiResponse<>("success", 200, "Onboarding updated successfully", onboardingService.update(id, param));
    }

    @PutMapping("/onboardings/{id}/steps/{stepId}")
    public ApiResponse<OnboardingDTO> updateStep(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @PathVariable String stepId,
            @Valid @RequestBody OnboardingActionDTOs.StepUpdate param) {
        return new ApiResponse<>("success", 200, "Step updated successfully",
                onboardingService.updateStep(id, stepId, param, userId));
    }

    @PostMapping("/onboardings/{id}/steps")
    public ApiResponse<OnboardingDTO> addStep(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody OnboardingActionDTOs.NewStep param) {
        return new ApiResponse<>("success", 200, "Step added successfully",
                onboardingService.addStep(id, param.label(), userId));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/onboardings/{id}/steps/{stepId}")
    public ApiResponse<OnboardingDTO> removeStep(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @PathVariable String stepId) {
        return new ApiResponse<>("success", 200, "Step removed successfully",
                onboardingService.removeStep(id, stepId, userId));
    }
}
