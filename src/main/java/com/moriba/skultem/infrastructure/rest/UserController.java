package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.usecase.CreateUserUseCase;
import com.moriba.skultem.application.usecase.GetUserUseCase;
import com.moriba.skultem.application.usecase.ListUserBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateUserDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final ListUserBySchoolUseCase listUserBySchoolUseCase;
    private final GetUserUseCase getUserUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<UserDTO> create(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateUserDTO param) {
        var res = createUserUseCase.execute(school, param.givenNames(), param.familyName(), param.email(),
                param.password(), param.role());
        return new ApiResponse<>("success", 200, "User created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
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

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<UserDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getUserUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "User fetched successfully", res);
    }

    @GetMapping("/me")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<UserDTO> me(
        @AuthenticationPrincipal(expression = "userId") String userId,
        @AuthenticationPrincipal(expression = "activeSchoolId") String school
    ) {
        var res = getUserUseCase.execute(school, userId);
        return new ApiResponse<>("success", 200, "User fetched successfully", res);
    }
}
