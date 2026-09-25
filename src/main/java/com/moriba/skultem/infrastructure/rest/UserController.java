package com.moriba.skultem.infrastructure.rest;
import org.springframework.web.bind.annotation.PutMapping;

import com.moriba.skultem.application.dto.StaffScopeDTO;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.application.usecase.AssignStaffManagementSectionsUseCase;
import com.moriba.skultem.application.usecase.ListStaffManagementSectionsUseCase;
import com.moriba.skultem.infrastructure.rest.dto.AssignStaffSectionsDTO;
import com.moriba.skultem.infrastructure.security.SectionNeutral;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AdminResetPasswordResultDTO;
import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.dto.UserPayrollStatusDTO;
import com.moriba.skultem.application.services.UserService;
import com.moriba.skultem.application.usecase.AdminResetPasswordUseCase;
import com.moriba.skultem.application.usecase.CreateUserUseCase;
import com.moriba.skultem.application.usecase.GetUserPayrollStatusUseCase;
import com.moriba.skultem.application.usecase.GetUserUseCase;
import com.moriba.skultem.application.usecase.IncludeUserInPayrollUseCase;
import com.moriba.skultem.application.usecase.ListUserBySchoolUseCase;
import com.moriba.skultem.application.usecase.RemoveRoleUseCase;
import com.moriba.skultem.application.usecase.ResetPasswordUseCase;
import com.moriba.skultem.application.usecase.SetUserAccessUseCase;
import com.moriba.skultem.application.usecase.UpdateUserPhotoUseCase;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.AssignRoleDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateUserDTO;
import com.moriba.skultem.infrastructure.rest.dto.IncludeUserInPayrollDTO;
import com.moriba.skultem.infrastructure.rest.dto.ResetPasswordDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final ListUserBySchoolUseCase listUserBySchoolUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final AdminResetPasswordUseCase adminResetPasswordUseCase;
    private final UserService svc;
    private final GetUserUseCase getUserUseCase;
    private final GetUserPayrollStatusUseCase getUserPayrollStatusUseCase;
    private final IncludeUserInPayrollUseCase includeUserInPayrollUseCase;
    private final UpdateUserPhotoUseCase updateUserPhotoUseCase;
    private final SetUserAccessUseCase setUserAccessUseCase;
    private final RemoveRoleUseCase removeRoleUseCase;
    private final AssignStaffManagementSectionsUseCase assignStaffManagementSectionsUseCase;
    private final ListStaffManagementSectionsUseCase listStaffManagementSectionsUseCase;
    private final SectionScopeService sectionScopeService;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @permissionService.canGrantRole(#school, #param.role())")
    public ApiResponse<UserDTO> create(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateUserDTO param) {
        boolean includeInPayroll = param.includeInPayroll() != null && param.includeInPayroll();

        var res = createUserUseCase.execute(school, param.givenNames(), param.familyName(), param.email(),
                param.role(), includeInPayroll, param.staffId(), param.phone(), param.street(), param.city(),
                param.gender(), param.title(), param.designation());
        return new ApiResponse<>("success", 200, "User created successfully", res);
    }

    @PostMapping("/assign")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @permissionService.canGrantRole(#school, #param.role())")
    public ApiResponse<UserDTO> assignRole(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody AssignRoleDTO param) {
        var res = svc.assignRole(school, param.userId(), param.role());
        return new ApiResponse<>("success", 200, "User asign successfully", res);
    }

    @SectionNeutral
    @PostMapping("/reset-password")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<UserDTO> resetPassword(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody ResetPasswordDTO param) {
        var payload = new ResetPasswordUseCase.ResetPassword(userId, param.password(), school);
        var res = resetPasswordUseCase.execute(payload);
        return new ApiResponse<>("success", 200, "User reset password successfully", res);
    }

    // An ADMIN/OWNER/PROPRIETOR issuing a brand new temporary password for a staff member -
    // e.g. they're locked out and can't reach the self-service resetPassword() above (which
    // only works while the caller's own account is already in RESET_PASSWORD state). Unlike
    // that endpoint, this one works on any staff account and puts it into RESET_PASSWORD, so
    // the temporaryPassword in the response is meant to be shared with them directly (call,
    // chat, in person) - they're forced through /reset-password on next login.
    // See AdminResetPasswordUseCase.
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @permissionService.canManageUser(#school, #id)")
    public ApiResponse<AdminResetPasswordResultDTO> adminResetPassword(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String actingUserId,
            @PathVariable String id) {
        var res = adminResetPasswordUseCase.execute(school, id, actingUserId);
        return new ApiResponse<>("success", 200, "Temporary password generated successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<UserDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listUserBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Users fetched successfully", list, meta);
    }

    // Deactivate (or reactivate) any user at this school - not just a Teacher/payroll record
    // (TeacherController has a narrower version of this scoped to staff). Flips every role the
    // target holds at this school together and signs out any session they're currently using.
    // Blocked from deactivating your own account or a SYSTEM_ADMIN. See SetUserAccessUseCase.
    @PatchMapping("/{id}/access")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @permissionService.canManageUser(#school, #id)")
    public ApiResponse<UserDTO> setAccess(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String actingUserId,
            @PathVariable String id,
            @RequestParam("active") boolean active) {
        var res = setUserAccessUseCase.execute(school, id, actingUserId, active);
        String message = active ? "User reactivated successfully" : "User deactivated successfully";
        return new ApiResponse<>("success", 200, message, res);
    }

    // Revoke one specific role from a user at this school (e.g. they keep Teacher but no longer
    // need Accountant) - the removal counterpart to /assign. See RemoveRoleUseCase.
    @DeleteMapping("/{id}/role")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @permissionService.canGrantRole(#school, #role) and @permissionService.canManageUser(#school, #id)")
    public ApiResponse<UserDTO> removeRole(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String actingUserId,
            @PathVariable String id,
            @RequestParam("role") String role) {
        var res = removeRoleUseCase.execute(school, id, actingUserId, Role.valueOf(role));
        return new ApiResponse<>("success", 200, "Role removed successfully", res);
    }

    @SectionNeutral
    @GetMapping("/notifications")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'TEACHER', 'PARENT', 'PROPRIETOR')")
    public ApiResponse<Object> notifications(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId) {
        var res = svc.openNotifications(userId, school);
        return new ApiResponse<>("success", 200, "Users notification successfully", res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<UserDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getUserUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "User fetched successfully", res);
    }

    @GetMapping("/{id}/payroll-status")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<UserPayrollStatusDTO> payrollStatus(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getUserPayrollStatusUseCase.execute(id);
        return new ApiResponse<>("success", 200, "Payroll status fetched successfully", res);
    }

    @PostMapping("/{id}/include-in-payroll")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<UserPayrollStatusDTO> includeInPayroll(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody IncludeUserInPayrollDTO param) {
        var res = includeUserInPayrollUseCase.execute(school, id, param.staffId(), param.phone(), param.street(),
                param.city(), param.gender(), param.title(), param.designation());
        return new ApiResponse<>("success", 200, "User included in payroll successfully", res);
    }

    // Covers Teacher, Parent and plain account photos alike - they're all backed by a User (see
    // UpdateUserPhotoUseCase). Student has its own equivalent on StudentController - a separate
    // aggregate with its own photo.
    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<UserDTO> updatePhoto(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestPart("photo") MultipartFile photo) {
        var res = updateUserPhotoUseCase.execute(school, id, photo);
        return new ApiResponse<>("success", 200, "Photo updated successfully", res);
    }

    // The caller's own management-section scope - what the frontend uses to decide what to show.
    @GetMapping("/me/scope")
    @SectionNeutral
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<StaffScopeDTO.Current> myScope(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var scope = sectionScopeService.current();
        var res = new StaffScopeDTO.Current(scope.wholeSchool(),
                scope.wholeSchool() ? List.of() : List.copyOf(scope.levels()), scope.sectionIds(), scope.sectionNames());
        return new ApiResponse<>("success", 200, "Scope fetched successfully", res);
    }

    // Which staff are limited to which management sections (anyone not listed is whole-school).
    @GetMapping("/management-sections")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<StaffScopeDTO>> listManagementSections(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = listStaffManagementSectionsUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Management access fetched successfully", res);
    }

    // Owner-level only, like editing the structure itself: otherwise an admin could widen their own
    // (or a colleague's) access past the limit the owner set.
    @PutMapping("/{id}/management-sections")
    @PreAuthorize("@permissionService.isSchoolLeadership(#school)")
    public ApiResponse<StaffScopeDTO> assignManagementSections(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody AssignStaffSectionsDTO param) {
        var res = assignStaffManagementSectionsUseCase.execute(school, id, param.role(), param.sectionIds());
        return new ApiResponse<>("success", 200, "Management access updated successfully", res);
    }

    @GetMapping("/me")
    @SectionNeutral
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<UserDTO> me(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getUserUseCase.execute(school, userId);
        return new ApiResponse<>("success", 200, "User fetched successfully", res);
    }

    // Self-service - any signed-in user (Teacher, Parent, Admin/Accountant/...) uploading their
    // own photo from their My Profile page, as opposed to updatePhoto() above which lets an admin
    // set it on someone else's behalf.
    @SectionNeutral
    @PatchMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<UserDTO> updateMyPhoto(
            @AuthenticationPrincipal(expression = "userId") String userId,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestPart("photo") MultipartFile photo) {
        var res = updateUserPhotoUseCase.execute(school, userId, photo);
        return new ApiResponse<>("success", 200, "Photo updated successfully", res);
    }
}
