package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.usecase.GetTeacherUseCase;
import com.moriba.skultem.application.usecase.ListNotificationByParentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.usecase.CreateParentUseCase;
import com.moriba.skultem.application.usecase.ListParentBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListStudentByParentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.CreateParentDTO;

@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
public class ParentController {
        private final CreateParentUseCase createParentUseCase;
        private final GetTeacherUseCase getTeacherUseCase;
        private final ListParentBySchoolUseCase listParentBySchoolUseCase;
        private final ListStudentByParentUseCase listStudentByParentUseCase;
        private final ListNotificationByParentUseCase listNotificationByParentUseCase;

        @PostMapping
        @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
        public ApiResponse<ParentDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateParentDTO param) {
                var res = createParentUseCase.execute(school, param.givenNames(), param.familyName(),
                                param.email(), param.phone(), param.street(), param.city(), param.fatherName(),
                                param.motherName());
                return new ApiResponse<>("success", 200,
                                "Parent created successfully. Password is generated automatically.", res);
        }

        @GetMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT')")
        public ApiResponse<List<ParentDTO>> listBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listParentBySchoolUseCase.execute(school, page, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Parents fetched successfully", list, meta);
        }

        @GetMapping("/students")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'PARENT', 'ACCOUNTANT')")
        public ApiResponse<List<StudentDTO>> listStudentBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @AuthenticationPrincipal(expression = "userId") String userId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listStudentByParentUseCase.execute(school, userId, page, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Parent students fetched successfully", list, meta);
        }

        @GetMapping("/notifications")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'PARENT')")
        public ApiResponse<List<NotificationDTO>> listNotificationBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @AuthenticationPrincipal(expression = "userId") String userId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listNotificationByParentUseCase.execute(school, userId, page, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Parent notifications fetched successfully", list, meta);
        }

        @GetMapping("/{id}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT')")
        public ApiResponse<TeacherDTO> get(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id) {
                var res = getTeacherUseCase.execute(id, school);
                return new ApiResponse<>("success", 200, "Teacher fetched successfully", res);
        }
}
