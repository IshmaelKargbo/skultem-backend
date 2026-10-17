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

import com.moriba.skultem.application.dto.SupportDTO;
import com.moriba.skultem.application.services.SupportService;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.SupportRequests;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Support tickets for Skultem staff. Platform-level: a school's own users never reach these.
@RestController
@RequestMapping("/api/v1/system/support")
@PreAuthorize("@permissionService.hasPlatformPermission('SUPPORT')")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService support;

    private static Map<String, Object> meta(Page<?> res) {
        return Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
    }

    @GetMapping("/tickets")
    public ApiResponse<List<SupportDTO.Ticket>> tickets(
            @RequestParam(required = false) String status, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignedTo, @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String search, @RequestParam(required = false) String view,
            @RequestParam(defaultValue = "10") Integer size, @RequestParam(defaultValue = "1") Integer page) {
        var res = support.list(new SupportService.Filter(status, priority, assignedTo, schoolId, search, view), page, Math.min(size, 100));
        return new ApiResponse<>("success", 200, "Tickets fetched successfully", res.getContent(), meta(res));
    }

    @GetMapping("/summary")
    public ApiResponse<SupportDTO.Summary> summary() {
        return new ApiResponse<>("success", 200, "Support summary fetched successfully", support.summary());
    }

    @GetMapping("/tickets/{id}")
    public ApiResponse<SupportDTO.Detail> ticket(@PathVariable String id) {
        return new ApiResponse<>("success", 200, "Ticket fetched successfully", support.get(id));
    }

    @PostMapping("/tickets")
    public ApiResponse<SupportDTO.Detail> create(@AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody SupportRequests.Create param) {
        return new ApiResponse<>("success", 200, "Ticket opened successfully", support.create(param, userId));
    }

    @PutMapping("/tickets/{id}")
    public ApiResponse<SupportDTO.Detail> update(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody SupportRequests.Update param) {
        return new ApiResponse<>("success", 200, "Ticket updated successfully", support.update(id, param, userId));
    }

    @PostMapping("/tickets/{id}/status")
    public ApiResponse<SupportDTO.Detail> status(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody SupportRequests.ChangeStatus param) {
        return new ApiResponse<>("success", 200, "Ticket status updated successfully",
                support.changeStatus(id, param.status(), param.resolutionNote(), userId));
    }

    @PostMapping("/tickets/{id}/messages")
    public ApiResponse<SupportDTO.Detail> message(@AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id, @Valid @RequestBody SupportRequests.AddMessage param) {
        return new ApiResponse<>("success", 200, "Message added successfully",
                support.addMessage(id, param.kind(), param.body(), userId));
    }
}
