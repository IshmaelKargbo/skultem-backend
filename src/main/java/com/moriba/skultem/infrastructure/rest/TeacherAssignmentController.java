package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AssignSubjectToTeacherDTO;
import com.moriba.skultem.application.dto.AssignTeacherDTO;
import com.moriba.skultem.application.usecase.AssignSubjectToTeacherUseCase;
import com.moriba.skultem.application.usecase.AssignTeacherToClassUseCase;
import com.moriba.skultem.application.usecase.AssignSubjectToTeacherUseCase.SubjectAssignment;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping({"/api/v1/teacher/assignment", "/api/v1/teacher-assignment"})
@RequiredArgsConstructor
public class TeacherAssignmentController {
    private final AssignTeacherToClassUseCase assignTeacherToClassUseCase;
    private final AssignSubjectToTeacherUseCase assignSubjectToTeacherUseCase;

    @PostMapping("/class/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<Object> assignToClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId,
            @Valid @RequestBody AssignTeacherDTO param) {
        assignTeacherToClassUseCase.execute(school, classId, param.teacherId(), param.sectionId(), param.streamId());
        return new ApiResponse<>("success", 200, "Teacher assigned to class successfully", null);
    }

    @PostMapping("/subject/{classSessionId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<Object> assignToStream(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classSessionId,
            @Valid @RequestBody AssignSubjectToTeacherDTO param) {
        List<SubjectAssignment> list = param.assignments().stream()
                .map(e -> new SubjectAssignment(e.id(), e.teacherId(), e.subjectId())).toList();
        assignSubjectToTeacherUseCase.execute(school, classSessionId, list);
        return new ApiResponse<>("success", 200, "Subject assigned to teacher successfully", null);
    }
}
