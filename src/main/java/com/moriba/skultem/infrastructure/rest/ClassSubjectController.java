package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionScoped;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.dto.ClassSubjectDTO;
import com.moriba.skultem.application.usecase.CreateClassSessionUseCase;
import com.moriba.skultem.application.usecase.GetClassSessionUseCase;
import com.moriba.skultem.application.usecase.ListClassSubjectByClassUseCase;
import com.moriba.skultem.application.usecase.ListClassSubjectBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateClassSessionDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/class-subject")
@RequiredArgsConstructor
public class ClassSubjectController {

    private final CreateClassSessionUseCase createClassSessionUseCase;
    private final ListClassSubjectBySchoolUseCase listClassSubjectBySchoolUseCase;
    private final ListClassSubjectByClassUseCase listClassSubjectByClassUseCase;
    private final GetClassSessionUseCase getClassSessionUseCase;

    @SectionScoped
    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.clazz(#school, #param.classId())")
    public ApiResponse<Object> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateClassSessionDTO param) {
        createClassSessionUseCase.execute(school, param.classId(), param.academicYear(), param.streamId(),
                param.sectionId());
        return new ApiResponse<>("success", 200, "Class session created successfully", null);
    }

    @SectionScoped
    @GetMapping("/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER') and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<List<ClassSubjectDTO>> listByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = true) String classId,
            @RequestParam(required = false) String stream,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = true, defaultValue = "10") Integer size) {
        var res = listClassSubjectByClassUseCase.execute(school, classId, stream, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Class subjects fetched successfully", list, meta);
    }

    @SectionScoped
    @GetMapping()
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER') and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<List<ClassSubjectDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) Boolean mandatory,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        var res = listClassSubjectBySchoolUseCase.execute(school, page - 1, size, classId, mandatory, query, sortBy,
                direction);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Class subjects fetched successfully", list, meta);
    }

    @SectionScoped
    @GetMapping("/one/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school) and @sectionScope.classSession(#school, #id)")
    public ApiResponse<ClassSessionDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getClassSessionUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Class session fetched successfully", res);
    }
}
