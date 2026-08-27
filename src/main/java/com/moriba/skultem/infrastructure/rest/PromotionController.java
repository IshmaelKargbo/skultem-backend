package com.moriba.skultem.infrastructure.rest;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.PromotionConfigDTO;
import com.moriba.skultem.application.dto.PromotionProgressDTO;
import com.moriba.skultem.application.dto.PromotionRequestDTO;
import com.moriba.skultem.application.dto.PromotionRosterDTO;
import com.moriba.skultem.application.usecase.ApprovePromotionRequestUseCase;
import com.moriba.skultem.application.usecase.CloseAcademicYearAndActivateNextUseCase;
import com.moriba.skultem.application.usecase.GetPromotionConfigUseCase;
import com.moriba.skultem.application.usecase.GetPromotionProgressUseCase;
import com.moriba.skultem.application.usecase.GetPromotionRequestUseCase;
import com.moriba.skultem.application.usecase.GetPromotionRosterUseCase;
import com.moriba.skultem.application.usecase.ListPromotionRequestsUseCase;
import com.moriba.skultem.application.usecase.ReturnPromotionRequestUseCase;
import com.moriba.skultem.application.usecase.SubmitPromotionRequestUseCase;
import com.moriba.skultem.application.usecase.UpdatePromotionConfigUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.ApprovePromotionRequestDTO;
import com.moriba.skultem.infrastructure.rest.dto.PromotionNoteDTO;
import com.moriba.skultem.infrastructure.rest.dto.SubmitPromotionRequestDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdatePromotionConfigDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/promotion")
@RequiredArgsConstructor
public class PromotionController {
    private final GetPromotionRosterUseCase getPromotionRosterUseCase;
    private final SubmitPromotionRequestUseCase submitPromotionRequestUseCase;
    private final ListPromotionRequestsUseCase listPromotionRequestsUseCase;
    private final GetPromotionRequestUseCase getPromotionRequestUseCase;
    private final ReturnPromotionRequestUseCase returnPromotionRequestUseCase;
    private final ApprovePromotionRequestUseCase approvePromotionRequestUseCase;
    private final GetPromotionProgressUseCase getPromotionProgressUseCase;
    private final CloseAcademicYearAndActivateNextUseCase closeAcademicYearAndActivateNextUseCase;
    private final GetPromotionConfigUseCase getPromotionConfigUseCase;
    private final UpdatePromotionConfigUseCase updatePromotionConfigUseCase;

    @GetMapping("/requests/session/{sessionId}/roster")
    @PreAuthorize("@permissionService.canPromoteClassSession(#school, #sessionId)")
    public ApiResponse<PromotionRosterDTO> promotionRoster(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String sessionId) {
        var res = getPromotionRosterUseCase.execute(school, sessionId);
        return new ApiResponse<>("success", 200, "Promotion roster fetched successfully", res);
    }

    @PostMapping("/requests/session/{sessionId}")
    @PreAuthorize("@permissionService.canPromoteClassSession(#school, #sessionId)")
    public ApiResponse<PromotionRequestDTO> submitPromotionRequest(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String sessionId,
            @Valid @RequestBody SubmitPromotionRequestDTO param) {
        var res = submitPromotionRequestUseCase.execute(school, sessionId, param.note(), param.items());
        return new ApiResponse<>("success", 200, "Promotion submitted for review", res);
    }

    @GetMapping("/requests/session/{sessionId}/current")
    @PreAuthorize("@permissionService.canPromoteClassSession(#school, #sessionId)")
    public ApiResponse<PromotionRequestDTO> currentPromotionRequestForSession(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String sessionId) {
        var res = getPromotionRequestUseCase.executeBySession(school, sessionId);
        return new ApiResponse<>("success", 200, "Promotion request fetched successfully", res);
    }

    @GetMapping("/requests/mine")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<Object> myPromotionRequests(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listPromotionRequestsUseCase.executeByUser(school, userId, page, size);
        return listResponse(res);
    }

    // --- Admin / proprietor: review queue ---

    @GetMapping("/requests")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<Object> listPromotionRequests(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String status,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listPromotionRequestsUseCase.execute(school, status, page, size);
        return listResponse(res);
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("@permissionService.canManagePromotionRequest(#school, #id)")
    public ApiResponse<PromotionRequestDTO> getPromotionRequest(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getPromotionRequestUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Promotion request fetched successfully", res);
    }

    @PostMapping("/requests/{id}/return")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<PromotionRequestDTO> returnPromotionRequest(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestBody PromotionNoteDTO param) {
        var res = returnPromotionRequestUseCase.execute(school, id, param.note());
        return new ApiResponse<>("success", 200, "Promotion request returned to the class master", res);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<PromotionRequestDTO> approvePromotionRequest(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestBody ApprovePromotionRequestDTO param) {
        var res = approvePromotionRequestUseCase.execute(school, id, param.note(), param.allowToPassEnrollmentIds());
        return new ApiResponse<>("success", 200, "Promotion approved and students moved", res);
    }

    @GetMapping("/progress")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<PromotionProgressDTO> promotionProgress(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getPromotionProgressUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Promotion progress fetched successfully", res);
    }

    @PostMapping("/close-year")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<Object> closeAcademicYear(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        closeAcademicYearAndActivateNextUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Academic year closed and next year activated", null);
    }

    // --- Admin / proprietor: promotion rules ---

    @GetMapping("/config")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<PromotionConfigDTO> promotionConfig(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getPromotionConfigUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Promotion rules fetched successfully", res);
    }

    @PostMapping("/config")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<PromotionConfigDTO> updatePromotionConfig(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody UpdatePromotionConfigDTO param) {
        var res = updatePromotionConfigUseCase.execute(school, param.minPassMark(), param.maxRepeatCount(),
                param.requireApproval(), param.requireRemarkForPromote());
        return new ApiResponse<>("success", 200, "Promotion rules updated successfully", res);
    }

    private ApiResponse<Object> listResponse(org.springframework.data.domain.Page<PromotionRequestDTO> res) {
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Promotion requests fetched successfully", list, meta);
    }
}
