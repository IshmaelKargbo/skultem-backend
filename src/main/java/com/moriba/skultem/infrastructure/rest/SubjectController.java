package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.usecase.CreateSubjectUseCase;
import com.moriba.skultem.application.usecase.GetSubjectUseCase;
import com.moriba.skultem.application.usecase.ListSubjectBySchoolUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateSubjectDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/subject")
@RequiredArgsConstructor
public class SubjectController {
    private final CreateSubjectUseCase createSubjectUseCase;
    private final ListSubjectBySchoolUseCase listSubjectBySchoolUseCase;
    private final GetSubjectUseCase getSubjectUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<SubjectDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateSubjectDTO param) {
        var res = createSubjectUseCase.execute(school, param.name(), param.code(), param.description());
        return new ApiResponse<>("success", 200, "Subject created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<SubjectDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listSubjectBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Subjects fetched successfully", list, meta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<SubjectDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getSubjectUseCase.execute(id);
        return new ApiResponse<>("success", 200, "Subject fetched successfully", res);
    }
}
