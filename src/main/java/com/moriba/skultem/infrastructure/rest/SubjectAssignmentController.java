package com.moriba.skultem.infrastructure.rest;

import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.usecase.AssignSubjectsToStreamUseCase;
import com.moriba.skultem.application.usecase.AssignSubjectsToClassUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.AssignSubjectsDTO;
import com.moriba.skultem.infrastructure.rest.dto.SubjectAssignmentDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/assignment", "/api/v1/subject-assignment"})
@RequiredArgsConstructor
public class SubjectAssignmentController {
    private final AssignSubjectsToClassUseCase assignSubjectsToClassUseCase;
    private final AssignSubjectsToStreamUseCase assignStreamSubjectsUseCase;

    @PostMapping("/class/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<Object> assignToClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId,
            @Valid @RequestBody AssignSubjectsDTO param) {
        var assignments = param.assignments().stream()
                .map(this::toClassAssignment)
                .collect(Collectors.toList());
        assignSubjectsToClassUseCase.execute(school, classId, assignments);
        return new ApiResponse<>("success", 200, "Subjects assigned to class successfully", null);
    }

    @PostMapping("/stream/{streamId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<Object> assignToStream(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String streamId,
            @Valid @RequestBody AssignSubjectsDTO param) {
        var assignments = param.assignments().stream()
                .map(this::toStreamAssignment)
                .collect(Collectors.toList());
        assignStreamSubjectsUseCase.execute(school, streamId, assignments);
        return new ApiResponse<>("success", 200, "Subjects assigned to stream successfully", null);
    }

    private AssignSubjectsToClassUseCase.SubjectAssignment toClassAssignment(SubjectAssignmentDTO dto) {
        return new AssignSubjectsToClassUseCase.SubjectAssignment(dto.subjectId(), dto.groupId(),
                dto.mandatory());
    }

    private AssignSubjectsToStreamUseCase.SubjectAssignment toStreamAssignment(SubjectAssignmentDTO dto) {
        return new AssignSubjectsToStreamUseCase.SubjectAssignment(dto.subjectId(), dto.groupId(),
                dto.mandatory());
    }
}
