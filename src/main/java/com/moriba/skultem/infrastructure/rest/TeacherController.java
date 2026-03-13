package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.usecase.CreateTeacherUseCase;
import com.moriba.skultem.application.usecase.GetTeacherSubjectUseCase;
import com.moriba.skultem.application.usecase.GetTeacherUseCase;
import com.moriba.skultem.application.usecase.ListTeacherBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListTeacherSubjectBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListTeacherSubjectByTeacherUseCase;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateTeacherDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.moriba.skultem.application.usecase.ListTeacherSubjectBySessionUseCase;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherController {
        private final CreateTeacherUseCase createTeacherUseCase;
        private final GetTeacherUseCase getTeacherUseCase;
        private final ListTeacherBySchoolUseCase listTeacherBySchoolUseCase;
        private final ListTeacherSubjectBySchoolUseCase listTeacherSubjectBySchoolUseCase;
        private final ListTeacherSubjectByTeacherUseCase listTeacherSubjectByTeacherUseCase;
        private final ListTeacherSubjectBySessionUseCase listTeacherSubjectBySessionUseCase;
        private final GetTeacherSubjectUseCase getTeacherSubjectUseCase;

        @PostMapping
        @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
        public ApiResponse<TeacherDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateTeacherDTO param) {
                var title = Title.valueOf(param.title());
                var gender = Gender.valueOf(param.gender());

                var res = createTeacherUseCase.execute(school, title, param.givenNames(), param.familyName(), gender,
                                param.staffId(),
                                param.email(), param.phone(), param.street(), param.city(), param.classMaster());
                return new ApiResponse<>("success", 200,
                                "Teacher created successfully. Password is generated automatically.", res);
        }

        @GetMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<List<TeacherDTO>> listBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listTeacherBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Teachers fetched successfully", list, meta);
        }

        @GetMapping("/subject")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<List<TeacherSubjectDTO>> listSubjectBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listTeacherSubjectBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Teacher subjects fetched successfully", list,
                                meta);
        }

        @GetMapping("/subject/{teacherId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<List<TeacherSubjectDTO>> listSubjectByTeacher(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String teacherId) {
                var list = listTeacherSubjectByTeacherUseCase.execute(school, teacherId);
                return new ApiResponse<>("success", 200, "Teacher subjects fetched successfully", list);
        }

        @GetMapping("/subject/session/{sessionId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<List<TeacherSubjectDTO>> listSubjectBySection(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String sessionId,
                        @RequestParam(required = true, defaultValue = "1") int page,
                        @RequestParam(required = true, defaultValue = "10") int size) {
                var res = listTeacherSubjectBySessionUseCase.execute(school, sessionId, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());
                return new ApiResponse<>("success", 200, "Teacher subjects fetched successfully", list, meta);
        }

        @GetMapping("/subject/detail/{teacherId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<TeacherSubjectDTO> oneSubjectByTeacher(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String teacherId) {
                var res = getTeacherSubjectUseCase.execute(school, teacherId);
                return new ApiResponse<>("success", 200, "Teacher subject fetched successfully", res);
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
