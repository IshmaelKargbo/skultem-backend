package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.usecase.CreateClassSessionUseCase;
import com.moriba.skultem.application.usecase.GetClassSessionUseCase;
import com.moriba.skultem.application.usecase.ListClassSessionBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateClassSessionDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.moriba.skultem.application.usecase.ListUnassignClassBySchoolUseCase;

@RestController
@RequestMapping({"/api/v1/class/session", "/api/v1/class-session"})
@RequiredArgsConstructor
public class ClassSessionController {

    private final CreateClassSessionUseCase createClassSessionUseCase;
    private final ListClassSessionBySchoolUseCase listClassSessionBySchoolUseCase;
    private final ListUnassignClassBySchoolUseCase listUnassignClassBySchoolUseCase;
    private final GetClassSessionUseCase getClassSessionUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<ClassSessionDTO> create(
        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
        @Valid @RequestBody CreateClassSessionDTO param) {
        createClassSessionUseCase.execute(school, param.classId(), param.academicYear(), param.streamId(), param.sectionId());
        return new ApiResponse<>("success", 200, "Class session created successfully", null);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<ClassSessionDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listClassSessionBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Class sessions fetched successfully", list, meta);
    }

    @GetMapping({"/unassign", "/unassigned"})
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<ClassSessionDTO>> listUnassign(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listUnassignClassBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Class sessions fetched successfully", list, meta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<ClassSessionDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getClassSessionUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Class session fetched successfully", res);
    }
}
