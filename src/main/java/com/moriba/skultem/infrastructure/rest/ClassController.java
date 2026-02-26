package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.dto.ClassOverviewDTO;
import com.moriba.skultem.application.dto.ClassSectionDTO;
import com.moriba.skultem.application.dto.ClassStreamDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.CreateClassUseCase;
import com.moriba.skultem.application.usecase.GetClassOverviewUseCase;
import com.moriba.skultem.application.usecase.GetClassUseCase;
import com.moriba.skultem.application.usecase.GetCurrentClassMasterUseCase;
import com.moriba.skultem.application.usecase.ListClassBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListClassSectionByClassUseCase;
import com.moriba.skultem.application.usecase.ListClassStreamByIdUseCase;
import com.moriba.skultem.application.usecase.NextClassUseCase;
import com.moriba.skultem.application.usecase.RemoveTeacherFromClassUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateClassDTO;
import com.moriba.skultem.infrastructure.rest.dto.NextClassDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/class")
@RequiredArgsConstructor
public class ClassController {

    private final CreateClassUseCase createClassUseCase;
    private final NextClassUseCase nextClassUseCase;
    private final ListClassBySchoolUseCase listClassBySchoolUseCase;
    private final ListClassStreamByIdUseCase listClassStreamByIdUseCase;
    private final ListClassSectionByClassUseCase listClassSectionByClassUseCase;
    private final GetClassUseCase getClassUseCase;
    private final GetClassOverviewUseCase getClassOverviewUseCase;
    private final GetCurrentClassMasterUseCase getCurrentClassMasterUseCase;
    private final RemoveTeacherFromClassUseCase removeTeacherFromClassUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<ClassDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateClassDTO param) {
        var res = createClassUseCase.execute(school, param.name(), param.levelOrder(), param.sections(),
                param.streams(), param.level());
        return new ApiResponse<>("success", 200, "Class created successfully", res);
    }

    @PutMapping("/next")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<ClassDTO> nextClass(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody NextClassDTO param) {
        if (param.id().equals(param.nextClass())) {
            throw new RuleException("Next class cannot be the same as current class");
        }

        var res = nextClassUseCase.execute(school, param.id(), param.nextClass());
        return new ApiResponse<>("success", 200, "Next class set successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<ClassDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listClassBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Classes fetched successfully", list, meta);
    }

    @GetMapping("/master/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<ClassMasterDTO>> getClassMasterByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId) {
        var res = getCurrentClassMasterUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Class masters fetched successfully", res);
    }

    @GetMapping("/section/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<ClassSectionDTO>> getClassSectionsByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId) {
        var res = listClassSectionByClassUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Class sections fetched successfully", res);
    }

    @GetMapping("/streams/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<ClassStreamDTO>> listStreams(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId) {
        var list = listClassStreamByIdUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Class streams fetched successfully", list);
    }

    @PostMapping("/master/remove/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<Object> removeAsClassMaster(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        removeTeacherFromClassUseCase.execute(id, school);
        return new ApiResponse<>("success", 200, "Class master removed successfully", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<ClassDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getClassUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Class fetched successfully", res);
    }

    @GetMapping("/{id}/overview")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<ClassOverviewDTO> overview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getClassOverviewUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Class overview fetched successfully", res);
    }
}
