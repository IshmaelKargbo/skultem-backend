package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.idempotency.Idempotent;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.usecase.ChangeEnrollmentClassUseCase;
import com.moriba.skultem.application.usecase.EnrollmentStudentsUseCase;
import com.moriba.skultem.application.usecase.EnrollmentStudentsUseCase.EnrollData;
import com.moriba.skultem.application.usecase.GetEnrollmentByStudentAndClassUseCase;
import com.moriba.skultem.application.usecase.ListEnrollmentByClassUseCase;
import com.moriba.skultem.application.usecase.ListStudentBySchoolUseCase;
import com.moriba.skultem.application.usecase.SelectClassSubjectsUseCase;
import com.moriba.skultem.application.usecase.SelectClassSubjectsUseCase.ClassSubjectSelection;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.ChangeEnrollmentClassDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateEnrollmentDTO;
import com.moriba.skultem.infrastructure.rest.dto.SelectedSubjectsDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final SelectClassSubjectsUseCase selectClassSubjectsUseCase;
    private final EnrollmentStudentsUseCase createEnrollmentUseCase;
    private final ListStudentBySchoolUseCase listStudentBySchoolUseCase;
    private final ListEnrollmentByClassUseCase listEnrollmentByClassUseCase;
    private final GetEnrollmentByStudentAndClassUseCase enrollmentByStudentAndClassUseCase;
    private final ChangeEnrollmentClassUseCase changeEnrollmentClassUseCase;

    @Idempotent(operation = "enrollment.create")
    @SectionScoped
    @PostMapping("/class")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.clazz(#school, #param.classId()) and @sectionScope.students(#school, #param.students())")
    public ApiResponse<Void> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateEnrollmentDTO param) {
        var args = new EnrollData(school, param.classId(), param.students(), param.sectionId(), param.streamId());
        createEnrollmentUseCase.execute(args);
        return new ApiResponse<>("success", 200, "Enrollment created successfully", null);
    }

    @SectionScoped
    @GetMapping("/student/{studentId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER') and @sectionScope.student(#school, #studentId)")
    public ApiResponse<EnrollmentDTO> getEnrollmentByClassAndSubject(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId,
            @RequestParam(required = false) String academicYearId) {
        var res = enrollmentByStudentAndClassUseCase.execute(studentId, school, academicYearId);
        return new ApiResponse<>("success", 200, "Enrollment fetched successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<List<StudentDTO>> listBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listStudentBySchoolUseCase.execute(school, academicYearId, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Students fetched successfully", list, meta);
    }

    @SectionScoped
    @GetMapping("/class/{classId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER') and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<List<StudentDTO>> listByClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = false) String classId,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(required = false) String stream,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listEnrollmentByClassUseCase.execute(school, classId, stream, academicYearId, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Students fetched by class successfully", list, meta);
    }

    @SectionScoped
    @PatchMapping("/{enrollmentId}/class")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.enrollment(#school, #enrollmentId) and @sectionScope.clazz(#school, #param.classId())")
    public ApiResponse<ChangeEnrollmentClassUseCase.ChangeClassResult> changeClass(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String enrollmentId,
            @Valid @RequestBody ChangeEnrollmentClassDTO param) {
        var res = changeEnrollmentClassUseCase.execute(school, enrollmentId, param.classId(), param.sectionId(),
                param.streamId());
        return new ApiResponse<>("success", 200, "Student class changed successfully", res);
    }

    @SectionScoped
    @PostMapping("/class/{enrollmentId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.enrollment(#school, #enrollmentId)")
    public ApiResponse<StudentDTO> enrolledClassSubjects(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String enrollmentId, @Valid @RequestBody SelectedSubjectsDTO param) {
        var args = new ClassSubjectSelection(school, enrollmentId, param.optionalSubjects());
        selectClassSubjectsUseCase.execute(args);
        return new ApiResponse<>("success", 200, "Enrollment subjects for student set successfully", null);
    }
}
