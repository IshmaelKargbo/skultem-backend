package com.moriba.skultem.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.dto.SystemAdminStatsDTO;
import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.dto.UserSchoolMembershipDTO;
import com.moriba.skultem.application.dto.UserWithSchoolsDTO;
import com.moriba.skultem.application.usecase.BootstrapSystemAdminUseCase;
import com.moriba.skultem.application.usecase.SearchUsersAcrossSchoolsUseCase;
import com.moriba.skultem.application.usecase.SetSchoolStatusUseCase;
import com.moriba.skultem.application.usecase.SetSchoolUserStatusUseCase;
import com.moriba.skultem.application.usecase.SystemAdminStatsUseCase;
import com.moriba.skultem.application.usecase.UpdateSchoolUseCase;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.BootstrapSystemAdminDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateSchoolDTO;

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
    private final UpdateSchoolUseCase updateSchoolUseCase;
    private final SetSchoolUserStatusUseCase setSchoolUserStatusUseCase;

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

    // Unlike SchoolController#update (which edits whichever school the caller is a member of, via
    // activeSchoolId), this edits an arbitrary school by id - the system-admin schools table isn't
    // scoped to any one tenant.
    @PutMapping("/school/{id}")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<SchoolDTO> updateSchool(@PathVariable("id") String schoolId,
            @Valid @RequestBody UpdateSchoolDTO param) {
        var address = new Address(param.region(), param.district(), param.chiefdom(), param.city(), param.street());
        var res = updateSchoolUseCase.execute(schoolId, param.name(), param.domain(), address);
        return new ApiResponse<SchoolDTO>("success", 200, "School updated successfully", res);
    }

    // Lists the System Admins roster - `query` is optional and, when blank, matches every one of
    // them (see UserJpaRepository#searchByRole's LIKE '%%'), so this doubles as both a plain
    // "browse every system admin" listing and a name/email search within that roster.
    @GetMapping("/users")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<List<UserWithSchoolsDTO>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = searchUsersAcrossSchoolsUseCase.execute(query, Role.SYSTEM_ADMIN, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Users fetched successfully", list, meta);
    }

    // Activates/deactivates one user's membership at one school - distinct from
    // updateSchoolStatus above (which (de)activates the whole school) and from the user's own
    // account-wide status (not exposed here) - a system admin acting on a membership found via
    // the cross-tenant lookup above shouldn't accidentally touch that same person's access to
    // every other school they belong to.
    @PutMapping("/school/{schoolId}/user/{userId}/status")
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<UserSchoolMembershipDTO> updateSchoolUserStatus(
            @PathVariable("schoolId") String schoolId,
            @PathVariable("userId") String userId,
            @RequestParam("status") String status) {
        var res = setSchoolUserStatusUseCase.execute(schoolId, userId, status);
        return new ApiResponse<>("success", 200, "User status updated successfully", res);
    }

    @PostMapping("/bootstrap")
    public ApiResponse<UserDTO> bootstrap(@Valid @RequestBody BootstrapSystemAdminDTO param) {
        var res = bootstrapSystemAdminUseCase.execute(param.token(), param.domain(), param.email(), param.password(),
                param.givenNames(), param.familyName());
        return new ApiResponse<>("success", 200, "System admin created successfully", res);
    }
}
