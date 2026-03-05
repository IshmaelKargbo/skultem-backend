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

import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.application.usecase.CreateSubjectGroupUseCase;
import com.moriba.skultem.application.usecase.ListSubjectGroupByClassUseCase;
import com.moriba.skultem.application.usecase.ListSubjectGroupBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListSubjectGroupByStreamUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateSubjectGroupDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/subject-group")
@RequiredArgsConstructor
public class SubjectGroupController {
        private final CreateSubjectGroupUseCase createSubjectGroupUseCase;
        private final ListSubjectGroupBySchoolUseCase listSubjectGroupBySchoolUseCase;
        private final ListSubjectGroupByClassUseCase listSubjectGroupByClassUseCase;
        private final ListSubjectGroupByStreamUseCase listSubjectGroupByStreamUseCase;

        @PostMapping
        @PreAuthorize("@permissionService.hasSchoolRole(#school, 'SCHOOL_ADMIN')")
        public ApiResponse<SubjectGroupDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateSubjectGroupDTO param) {
                var res = createSubjectGroupUseCase.execute(school, param.name(), param.classId(),
                                param.streamId(), param.totalSelection());
                return new ApiResponse<>("success", 200, "Subject group created successfully", res);
        }

        @GetMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<List<SubjectGroupDTO>> list(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listSubjectGroupBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Subject groups fetched successfully", list,
                                meta);
        }

        @GetMapping("/class/{classId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<List<SubjectGroupDTO>> listByClass(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(required = true) String classId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listSubjectGroupByClassUseCase.execute(school, classId, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Subject groups by class fetched successfully", list,
                                meta);
        }

        @GetMapping("/stream/{streamId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SCHOOL_ADMIN', 'TEACHER')")
        public ApiResponse<List<SubjectGroupDTO>> listByStream(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(required = true) String streamId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listSubjectGroupByStreamUseCase.execute(school, streamId, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Subject groups by stream fetched successfully", list,
                                meta);
        }
}
