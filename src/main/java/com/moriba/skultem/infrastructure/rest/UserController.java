package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.dto.UserPayrollStatusDTO;
import com.moriba.skultem.application.services.UserService;
import com.moriba.skultem.application.usecase.CreateUserUseCase;
import com.moriba.skultem.application.usecase.GetUserPayrollStatusUseCase;
import com.moriba.skultem.application.usecase.GetUserUseCase;
import com.moriba.skultem.application.usecase.IncludeUserInPayrollUseCase;
import com.moriba.skultem.application.usecase.ListUserBySchoolUseCase;
import com.moriba.skultem.application.usecase.ResetPasswordUseCase;
import com.moriba.skultem.application.usecase.UpdateUserPhotoUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.AssignRoleDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateUserDTO;
import com.moriba.skultem.infrastructure.rest.dto.IncludeUserInPayrollDTO;
import com.moriba.skultem.infrastructure.rest.dto.ResetPasswordDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
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
    private final UserService svc;
    private final GetUserUseCase getUserUseCase;
    private final GetUserPayrollStatusUseCase getUserPayrollStatusUseCase;
    private final IncludeUserInPayrollUseCase includeUserInPayrollUseCase;
    private final UpdateUserPhotoUseCase updateUserPhotoUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<UserDTO> create(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateUserDTO param) {
        boolean includeInPayroll = param.includeInPayroll() != null && param.includeInPayroll();

        var res = createUserUseCase.execute(school, param.givenNames(), param.familyName(), param.email(),
                param.role(), includeInPayroll, param.staffId(), param.phone(), param.street(), param.city(),
                param.gender(), param.title(), param.designation());
        return new ApiResponse<>("success", 200, "User created successfully", res);
    }

    @PostMapping("/assign")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<UserDTO> assignRole(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody AssignRoleDTO param) {
        var res = svc.assignRole(school, param.userId(), param.role());
        return new ApiResponse<>("success", 200, "User asign successfully", res);
    }

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

    @GetMapping("/me")
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
