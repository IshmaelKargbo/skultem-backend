package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.UserSessionDTO;
import com.moriba.skultem.application.mapper.UserSessionViewMapper;
import com.moriba.skultem.application.usecase.ListUserSessionBySchoolUseCase;
import com.moriba.skultem.application.usecase.LogoutUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class UserSessionController {
    private final ListUserSessionBySchoolUseCase listUserSessionBySchoolUseCase;
    private final LogoutUseCase logoutUseCase;

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<List<UserSessionDTO>> listBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listUserSessionBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent().stream().map(UserSessionViewMapper::toDTO).toList();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "User sessions fetched successfully", list, meta);
    }

    @PostMapping("/{sessionId}/logout")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<Object> logoutSession(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String sessionId) {
        logoutUseCase.execute(sessionId);
        return new ApiResponse<>("success", 200, "Session logged out successfully", null);
    }
}
