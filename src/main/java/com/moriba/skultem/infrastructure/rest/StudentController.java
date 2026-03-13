package com.moriba.skultem.infrastructure.rest;

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

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.dto.StudentFinanceOverviewDTO;
import com.moriba.skultem.application.usecase.CreateStudentUseCase;
import com.moriba.skultem.application.usecase.CreateStudentUseCase.CreateStudent;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.application.usecase.GetStudentFinanceOverviewUseCase;
import com.moriba.skultem.application.usecase.GetStudentUseCase;
import com.moriba.skultem.application.usecase.ListStudentBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListSubjectFeesByStudentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateStudentDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {
    private final CreateStudentUseCase createStudentUseCase;
    private final GetStudentUseCase getStudentUseCase;
    private final GetStudentFinanceOverviewUseCase getStudentFinanceOverviewUseCase;
    private final ListStudentBySchoolUseCase listStudentBySchoolUseCase;
    private final ListSubjectFeesByStudentUseCase listSubjectFeesByStudentUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
    public ApiResponse<StudentDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateStudentDTO param) {
        var args = new CreateStudent(param.classSessionId(), school, param.givenNames(), param.familyName(), param.academicNumber(),
                Gender.valueOf(param.gender()), param.parentId(), param.dateOfBirth());
        var res = createStudentUseCase.execute(args, param.selectedOptionIds());
        return new ApiResponse<>("success", 200, "Student created successfully", res);
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

    @GetMapping("/fee/{studentId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT')")
    public ApiResponse<List<StudentFeeDTO>> listStudentFees(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listSubjectFeesByStudentUseCase.execute(school, studentId, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Student fees fetched successfully", list, meta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<StudentDTO> get(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getStudentUseCase.execute(id, school);
        return new ApiResponse<>("success", 200, "Student fetched successfully", res);
    }

    @GetMapping("/{id}/finance-overview")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT')")
    public ApiResponse<StudentFinanceOverviewDTO> financeOverview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "10") Integer recentPayments) {
        var size = Math.max(1, recentPayments);
        var res = getStudentFinanceOverviewUseCase.execute(school, id, size);
        return new ApiResponse<>("success", 200, "Student finance overview fetched successfully", res);
    }
}
