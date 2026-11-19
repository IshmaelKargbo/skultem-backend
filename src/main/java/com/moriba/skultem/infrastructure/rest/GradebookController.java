package com.moriba.skultem.infrastructure.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.GradebookDTO;
import com.moriba.skultem.application.usecase.GetClassGradebookUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import lombok.RequiredArgsConstructor;

// The class master's grade book: every pupil's submitted marks in every subject, for one class section and term.
@RestController
@RequestMapping("/api/v1/gradebook")
@RequiredArgsConstructor
public class GradebookController {

    private final GetClassGradebookUseCase useCase;

    @SectionScoped
    @GetMapping("/session/{classSessionId}")
    @PreAuthorize("@permissionService.canViewGradebook(#school, #classSessionId) "
            + "and @sectionScope.classSession(#school, #classSessionId)")
    public ApiResponse<GradebookDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classSessionId,
            @RequestParam(required = false) String termId) {
        return new ApiResponse<>("success", 200, "Grade book fetched successfully",
                useCase.execute(school, classSessionId, termId));
    }
}
