package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.usecase.EnrollmentStudentsUseCase;
import com.moriba.skultem.application.usecase.GetEnrollmentByStudentAndClassUseCase;
import com.moriba.skultem.application.usecase.EnrollmentStudentsUseCase.EnrollData;
import com.moriba.skultem.application.usecase.ListStudentBySchoolUseCase;
import com.moriba.skultem.application.usecase.SelectClassSubjectsUseCase;
import com.moriba.skultem.application.usecase.SelectClassSubjectsUseCase.ClassSubjectSelection;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateEnrollmentDTO;
import com.moriba.skultem.infrastructure.rest.dto.SelectedSubjectsDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final SelectClassSubjectsUseCase selectClassSubjectsUseCase;
    private final EnrollmentStudentsUseCase createEnrollmentUseCase;
    private final ListStudentBySchoolUseCase listStudentBySchoolUseCase;
    private final GetEnrollmentByStudentAndClassUseCase enrollmentByStudentAndClassUseCase;

    @PostMapping("/class")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<Void> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateEnrollmentDTO param) {
        var args = new EnrollData(school, param.classId(), param.students(), param.sectionId(), param.streamId());
        createEnrollmentUseCase.execute(args);
        return new ApiResponse<>("success", 200, "Enrollment created successfully", null);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<EnrollmentDTO> getEnrollmentByClassAndSubject(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId) {
        var res = enrollmentByStudentAndClassUseCase.execute(studentId, school);
        return new ApiResponse<>("success", 200, "Enrollment fetched successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<List<StudentDTO>> listBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listStudentBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Students fetched successfully", list, meta);
    }

    @PostMapping("/class/{enrollmentId}")
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<StudentDTO> enrolledClassSubjects(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String enrollmentId, @Valid @RequestBody SelectedSubjectsDTO param) {
        var args = new ClassSubjectSelection(school, enrollmentId, param.optionalSubjects());
        selectClassSubjectsUseCase.execute(args);
        return new ApiResponse<>("success", 200, "Enrollment subjects for student set successfully", null);
    }
}
