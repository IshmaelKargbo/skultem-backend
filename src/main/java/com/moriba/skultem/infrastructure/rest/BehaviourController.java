package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.moriba.skultem.application.dto.BehaviourCategoryDTO;
import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.usecase.CreateBehaviourCategoryUseCase;
import com.moriba.skultem.application.usecase.CreateBehaviourUseCase;
import com.moriba.skultem.application.usecase.ListBehaviourBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListBehaviourCategoryBySchoolUseCase;
import com.moriba.skultem.application.usecase.ReportBehaviourByClassUseCase;
import com.moriba.skultem.domain.vo.Kind;
import com.moriba.skultem.domain.vo.KindCount;
import com.moriba.skultem.infrastructure.rest.dto.CreateBehaviourCategoryDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateBehaviourDTO;

@RestController
@RequestMapping("/api/v1/behaviours")
@RequiredArgsConstructor
public class BehaviourController {
        private final CreateBehaviourUseCase createBehaviourUseCase;
        private final CreateBehaviourCategoryUseCase createBehaviourCategoryUseCase;
        private final ListBehaviourBySchoolUseCase listBehaviourBySchoolUseCase;
        private final ReportBehaviourByClassUseCase reportBehaviourByClassUseCase;
        private final ListBehaviourCategoryBySchoolUseCase listBehaviourCategoryBySchoolUseCase;

        @PostMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'TEACHER', 'PROPRIETOR')")
        public ApiResponse<BehaviourDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateBehaviourDTO param) {
                var res = createBehaviourUseCase.execute(school, param.enrollment(), param.category(), Kind.valueOf(param.kind()), param.note());
                return new ApiResponse<>("success", 200,
                                "Bhaviour created successfully.", res);
        }

        @PostMapping("/category")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
        public ApiResponse<BehaviourCategoryDTO> createCategory(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateBehaviourCategoryDTO param) {
                var res = createBehaviourCategoryUseCase.execute(school, param.name(), param.description());
                return new ApiResponse<>("success", 200,
                                "Bhaviour category created successfully.", res);
        }

        @GetMapping("/category")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
        public ApiResponse<List<BehaviourCategoryDTO>> listCagtoryBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listBehaviourCategoryBySchoolUseCase.execute(school, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Behaviour categories fetched successfully", list, meta);
        }

        @GetMapping()
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
        public ApiResponse<List<BehaviourDTO>> listBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true) String classId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listBehaviourBySchoolUseCase.execute(school, classId, page - 1, size);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Behaviours fetched successfully", list, meta);
        }

        @GetMapping("/report")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
        public ApiResponse<List<KindCount>> listReportBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true) String classId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = reportBehaviourByClassUseCase.execute(school, classId, page - 1, size);
                return new ApiResponse<>("success", 200, "Behaviour report fetched successfully", res);
        }
}
