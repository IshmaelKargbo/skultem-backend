package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.PendingPickupDTO;
import com.moriba.skultem.application.usecase.GetPendingPickupsUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

// Combines fee-entitled Supply and unfulfilled MaterialSale records into one "who still needs to
// collect something" list - see GetPendingPickupsUseCase for why this reads both instead of
// merging them. Nested under /materials since both underlying concepts are.
@RestController
@RequestMapping("/api/v1/materials/pending-pickups")
@RequiredArgsConstructor
public class PendingPickupController {

    private final GetPendingPickupsUseCase getPendingPickupsUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<PendingPickupDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = getPendingPickupsUseCase.execute(school, page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Pending pickups fetched successfully", list, meta);
    }
}
