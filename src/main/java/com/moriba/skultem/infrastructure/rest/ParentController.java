package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ParentMapper;
import com.moriba.skultem.application.usecase.DeleteParentPermanentlyUseCase;
import com.moriba.skultem.application.usecase.EditParentUseCase;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.infrastructure.rest.dto.DeleteParentDTO;
import com.moriba.skultem.infrastructure.rest.dto.EditParentDTO;
import com.moriba.skultem.infrastructure.security.SectionNeutral;
import com.moriba.skultem.infrastructure.security.SectionScoped;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.usecase.AddParentEmailUseCase;
import com.moriba.skultem.application.usecase.ListNotificationByParentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.dto.ParentRequest;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.usecase.CreateParentUseCase;
import com.moriba.skultem.application.usecase.ListParentBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListStudentByParentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.AddParentEmailDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateParentDTO;

@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
public class ParentController {
        private final CreateParentUseCase createParentUseCase;
        private final AddParentEmailUseCase addParentEmailUseCase;
        private final EditParentUseCase editParentUseCase;
        private final DeleteParentPermanentlyUseCase deleteParentPermanentlyUseCase;
        private final ParentRepository parentRepo;
        private final ListParentBySchoolUseCase listParentBySchoolUseCase;
        private final ListStudentByParentUseCase listStudentByParentUseCase;
        private final ListNotificationByParentUseCase listNotificationByParentUseCase;

        // A new parent has no children yet, so adding one is section-neutral; they show up for a
        // section-limited admin once a child in their section is linked (or while they have none).
        @SectionNeutral
        @PostMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
        public ApiResponse<ParentDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateParentDTO param) {
                var payload = new ParentRequest(school, param.givenNames(), param.familyName(), param.email(),
                                param.phone(), param.street(), param.city());
                var res = createParentUseCase.execute(payload);
                return new ApiResponse<>("success", 200,
                                "Parent created successfully. Password is generated automatically.", res);
        }

        @SectionScoped
        @GetMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'ACCOUNTANT')")
        public ApiResponse<List<ParentDTO>> listBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = false) String query,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String direction,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listParentBySchoolUseCase.execute(school, page, size, query, sortBy, direction);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Parents fetched successfully", list, meta);
        }

        @GetMapping("/students")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'PARENT', 'ACCOUNTANT')")
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

        @SectionScoped
        @PatchMapping("/{id}/email")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.parent(#school, #id)")
        public ApiResponse<ParentDTO> addEmail(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @Valid @RequestBody AddParentEmailDTO param) {
                var res = addParentEmailUseCase.execute(school, id, param.email());
                return new ApiResponse<>("success", 200,
                                "Email added. The parent now has portal access.", res);
        }

        @SectionScoped
        @GetMapping("/{id}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'ACCOUNTANT') and @sectionScope.parent(#school, #id)")
        public ApiResponse<ParentDTO> get(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id) {
                var parent = parentRepo.findByIdAndSchoolId(id, school)
                                .orElseThrow(() -> new NotFoundException("Parent not found"));
                return new ApiResponse<>("success", 200, "Parent fetched successfully", ParentMapper.toDTO(parent));
        }

        @SectionScoped
        @PatchMapping("/{id}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.parent(#school, #id)")
        public ApiResponse<ParentDTO> edit(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @Valid @RequestBody EditParentDTO param) {
                var res = editParentUseCase.execute(school, id, param.givenNames(), param.familyName(), param.phone(),
                                param.street(), param.city(), param.email());
                return new ApiResponse<>("success", 200, "Parent updated successfully", res);
        }

        // Owner-level only: this can't be undone. The guardian's full name in the body is the confirmation.
        @SectionScoped
        @PostMapping("/{id}/delete-permanently")
        @PreAuthorize("@permissionService.isSchoolLeadership(#school) and @sectionScope.parent(#school, #id)")
        public ApiResponse<DeleteParentPermanentlyUseCase.Result> deletePermanently(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @Valid @RequestBody DeleteParentDTO param) {
                var res = deleteParentPermanentlyUseCase.execute(school, id, param.confirmation());
                return new ApiResponse<>("success", 200, res.parentName() + " was permanently deleted", res);
        }
}
