package com.moriba.skultem.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.dto.SystemAdminStatsDTO;
import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.dto.UserWithSchoolsDTO;
import com.moriba.skultem.application.usecase.BootstrapSystemAdminUseCase;
import com.moriba.skultem.application.usecase.SearchUsersAcrossSchoolsUseCase;
import com.moriba.skultem.application.usecase.SetSchoolStatusUseCase;
import com.moriba.skultem.application.usecase.SystemAdminStatsUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.BootstrapSystemAdminDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemAdminController {
    private final SystemAdminStatsUseCase statsUseCase;
    private final SetSchoolStatusUseCase setSchoolStatusUseCase;
    private final BootstrapSystemAdminUseCase bootstrapSystemAdminUseCase;
    private final SearchUsersAcrossSchoolsUseCase searchUsersAcrossSchoolsUseCase;

    @GetMapping("/stats")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<SystemAdminStatsDTO> stats() {
        var res = statsUseCase.execute();
        return new ApiResponse<SystemAdminStatsDTO>("success", 200, "System stats fetched successfully", res);
    }

    @PutMapping("/school/{id}/status")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<SchoolDTO> updateSchoolStatus(@PathVariable("id") String schoolId,
            @RequestParam("status") String status) {
        var res = setSchoolStatusUseCase.execute(schoolId, status);
        return new ApiResponse<SchoolDTO>("success", 200, "School status updated successfully", res);
    }

    // Cross-tenant user lookup - e.g. helping a locked-out school owner, or checking which
    // schools an email address already belongs to. See SearchUsersAcrossSchoolsUseCase.
    @GetMapping("/users")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<List<UserWithSchoolsDTO>> searchUsers(
            @RequestParam("query") String query,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = searchUsersAcrossSchoolsUseCase.execute(query, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Users fetched successfully", list, meta);
    }

    // Reachable pre-auth - see SecurityConfig and BootstrapSystemAdminUseCase for how this stays
    // safe without @PreAuthorize.
    @PostMapping("/bootstrap")
    public ApiResponse<UserDTO> bootstrap(@Valid @RequestBody BootstrapSystemAdminDTO param) {
        var res = bootstrapSystemAdminUseCase.execute(param.token(), param.domain(), param.email(), param.password(),
                param.givenNames(), param.familyName());
        return new ApiResponse<>("success", 200, "System admin created successfully", res);
    }
}
