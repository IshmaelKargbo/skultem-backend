package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.PlaygroundSummaryDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.usecase.GetPlaygroundSummaryUseCase;
import com.moriba.skultem.application.usecase.WipeTestSchoolDataUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.GoLiveDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// A school's own view of its playground mode (the flag itself is only ever set by a system admin -
// see SystemAdminController). Deliberately not under /api/v1/school/**, which SecurityConfig
// leaves open pre-auth for signup - this should never be reachable without a logged-in caller.
@RestController
@RequestMapping("/api/v1/playground")
@RequiredArgsConstructor
public class PlaygroundController {
    private final GetPlaygroundSummaryUseCase getPlaygroundSummaryUseCase;
    private final WipeTestSchoolDataUseCase wipeTestSchoolDataUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<PlaygroundSummaryDTO> summary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getPlaygroundSummaryUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Playground summary fetched successfully", res);
    }

    // Going live is the school leadership's call: the owner, a proprietor or a SUPER_ADMIN (see
    // PermissionService#isSchoolLeadership). Plain admins can see the summary above but not act on it.
    @PostMapping("/go-live")
    @PreAuthorize("@permissionService.isSchoolLeadership(#school)")
    public ApiResponse<SchoolDTO> goLive(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody GoLiveDTO param) {
        var res = wipeTestSchoolDataUseCase.execute(school, param.categories(), param.confirmation());
        return new ApiResponse<>("success", 200, "School is now live", res);
    }
}
