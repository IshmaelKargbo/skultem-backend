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

import com.moriba.skultem.application.dto.ClassAttentionDTO;
import com.moriba.skultem.application.dto.ClassAttentionSummaryDTO;
import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.dto.ClassOverviewDTO;
import com.moriba.skultem.application.dto.ClassSectionDTO;
import com.moriba.skultem.application.dto.ClassStreamDTO;
import com.moriba.skultem.application.dto.ClassSubjectResponse;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.ComputeClassAttentionUseCase;
import com.moriba.skultem.application.usecase.CreateClassUseCase;
import com.moriba.skultem.application.usecase.ListClassesNeedingAttentionUseCase;
import com.moriba.skultem.application.usecase.GetClassOverviewUseCase;
import com.moriba.skultem.application.usecase.GetClassSubjectUseCase;
import com.moriba.skultem.application.usecase.GetClassUseCase;
import com.moriba.skultem.application.usecase.GetCurrentClassMasterUseCase;
import com.moriba.skultem.application.usecase.ListClassBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListClassSectionByClassUseCase;
import com.moriba.skultem.application.usecase.ListClassStreamByIdUseCase;
import com.moriba.skultem.application.usecase.NextClassUseCase;
import com.moriba.skultem.application.usecase.RemoveTeacherFromClassUseCase;
import com.moriba.skultem.application.usecase.UpdateClassTemplateUseCase;
import com.moriba.skultem.application.usecase.UpdateClassTerminalUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateClassDTO;
import com.moriba.skultem.infrastructure.rest.dto.NextClassDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateClassTemplateDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateClassTerminalDTO;

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
    private final GetClassSubjectUseCase getClassSubjectUseCase;
    private final GetClassOverviewUseCase getClassOverviewUseCase;
    private final ComputeClassAttentionUseCase computeClassAttentionUseCase;
    private final ListClassesNeedingAttentionUseCase listClassesNeedingAttentionUseCase;
    private final GetCurrentClassMasterUseCase getCurrentClassMasterUseCase;
    private final RemoveTeacherFromClassUseCase removeTeacherFromClassUseCase;
    private final UpdateClassTemplateUseCase updateClassTemplateUseCase;
    private final UpdateClassTerminalUseCase updateClassTerminalUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ClassDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateClassDTO param) {
        var res = createClassUseCase.execute(school, param.name(), param.levelOrder(), param.sections(),
                param.streams(), param.assessmentTemplateId(), param.level());
        return new ApiResponse<>("success", 200, "Class created successfully", res);
    }

    @PutMapping("/next")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ClassDTO> nextClass(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody NextClassDTO param) {
        if (param.id().equals(param.nextClass())) {
            throw new RuleException("Next class cannot be the same as current class");
        }

        var res = nextClassUseCase.execute(school, param.id(), param.nextClass());
        return new ApiResponse<>("success", 200, "Next class set successfully", res);
    }

    // Registered ahead of GetMapping("/{id}") is unnecessary here - Spring MVC always prefers a
    // literal path segment over a variable one, but the explicit segment ("attention-summary" -
    // not just "attention", which would collide with GetMapping("/{id}/attention")'s pattern under
    // a different base) makes that not even a close call.
    @GetMapping("/attention-summary")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ClassAttentionSummaryDTO> attentionSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId) {
        var res = listClassesNeedingAttentionUseCase.execute(school, academicYearId);
        return new ApiResponse<>("success", 200, "Class attention summary fetched successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
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

    @GetMapping("/subject/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<ClassSubjectResponse> listClassSubject(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId,
            @RequestParam(required = false) String streamId) {
        var res = getClassSubjectUseCase.execute(school, classId, streamId);
        return new ApiResponse<>("success", 200, "Class masters fetched successfully", res);
    }

    @GetMapping("/master/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<ClassMasterDTO>> getClassMasterByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId,
            @RequestParam(required = false) String academicYearId) {
        var res = getCurrentClassMasterUseCase.executeDTO(school, classId, academicYearId);
        return new ApiResponse<>("success", 200, "Class masters fetched successfully", res);
    }

    @GetMapping("/section/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<ClassSectionDTO>> getClassSectionsByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId) {
        var res = listClassSectionByClassUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Class sections fetched successfully", res);
    }

    @GetMapping("/streams/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<ClassStreamDTO>> listStreams(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId) {
        var list = listClassStreamByIdUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Class streams fetched successfully", list);
    }

    @PostMapping("/master/remove/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
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
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<ClassOverviewDTO> overview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getClassOverviewUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Class overview fetched successfully", res);
    }

    @GetMapping("/{id}/attention")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<ClassAttentionDTO> attention(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestParam(required = false) String academicYearId) {
        var res = computeClassAttentionUseCase.execute(school, id, academicYearId);
        return new ApiResponse<>("success", 200, "Class attention fetched successfully", res);
    }

    @PutMapping("/{id}/terminal")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ClassDTO> updateTerminal(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateClassTerminalDTO param) {
        var res = updateClassTerminalUseCase.execute(school, id, param.terminal());
        return new ApiResponse<>("success", 200, "Class updated successfully", res);
    }

    @PutMapping("/{id}/template")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ClassDTO> updateTemplate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateClassTemplateDTO param) {
        var res = updateClassTemplateUseCase.execute(school, id, param.templateId());
        return new ApiResponse<>("success", 200, "Class template updated successfully", res);
    }
}
