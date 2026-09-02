package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.moriba.skultem.application.dto.LeaveRequestDTO;
import com.moriba.skultem.application.dto.LeaveSummaryDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.services.LeaveService;
import com.moriba.skultem.domain.model.LeaveRequest;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateLeaveRequestDTO;
import com.moriba.skultem.infrastructure.rest.dto.ReviewLeaveRequestDTO;
import com.moriba.skultem.infrastructure.rest.mapper.MetaMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveService leaveService;

    @PostMapping("/me")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'TEACHER')")
    public ApiResponse<LeaveRequestDTO> createForMe(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody CreateLeaveRequestDTO param) {
        var res = leaveService.createForMe(school, userId, param.type(), param.startDate(), param.endDate(),
                param.reason());
        return new ApiResponse<>("success", 200, "Leave request submitted successfully", res);
    }

    @GetMapping("/me")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'TEACHER')")
    public ApiResponse<List<LeaveRequestDTO>> listMine(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = leaveService.listMine(school, userId, page, size);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Leave requests fetched successfully", res.getContent(), meta);
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<LeaveRequestDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateLeaveRequestDTO param) {
        if (param.teacherId() == null || param.teacherId().isBlank()) {
            throw new BadRequestException("Teacher is required");
        }

        var res = leaveService.create(school, param.teacherId(), param.type(), param.startDate(), param.endDate(),
                param.reason());
        return new ApiResponse<>("success", 200, "Leave request created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<LeaveRequestDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) LeaveRequest.Status status,
            @RequestParam(required = false) LeaveRequest.Type type,
            @RequestParam(required = false) String search) {
        var res = leaveService.list(school, page, size, status, type, search);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Leave requests fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/summary")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<LeaveSummaryDTO> summary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return new ApiResponse<>("success", 200, "Leave summary fetched successfully", leaveService.summary(school));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<LeaveRequestDTO> getById(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        return new ApiResponse<>("success", 200, "Leave request fetched successfully", leaveService.getById(school, id));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<LeaveRequestDTO> review(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody ReviewLeaveRequestDTO param) {
        var res = leaveService.review(school, id, param.approve(), param.note());
        return new ApiResponse<>("success", 200, "Leave request reviewed successfully", res);
    }
}
