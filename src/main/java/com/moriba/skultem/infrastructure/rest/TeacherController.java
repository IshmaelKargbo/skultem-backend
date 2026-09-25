package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionNeutral;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.TeacherClassMasterDTO;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.services.TeacherService;
import com.moriba.skultem.application.services.TeacherService.TeacherRecord;
import com.moriba.skultem.application.usecase.CreateTeacherUseCase;
import com.moriba.skultem.application.usecase.GetTeacherSubjectUseCase;
import com.moriba.skultem.application.usecase.ListMyClassMasterAssignmentsUseCase;
import com.moriba.skultem.application.usecase.ListTeacherSubjectBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListTeacherSubjectByTeacherUseCase;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateTeacherDTO;
import com.moriba.skultem.infrastructure.rest.dto.EditTeacherDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
        private final ListTeacherSubjectBySchoolUseCase listTeacherSubjectBySchoolUseCase;
        private final ListTeacherSubjectByTeacherUseCase listTeacherSubjectByTeacherUseCase;
        private final ListTeacherSubjectBySessionUseCase listTeacherSubjectBySessionUseCase;
        private final ListMyClassMasterAssignmentsUseCase listMyClassMasterAssignmentsUseCase;
        private final TeacherService teacherSvc;
        private final GetTeacherSubjectUseCase getTeacherSubjectUseCase;

        // A teacher is a staff record with no level of its own, so adding one is section-neutral - a
        // section-limited admin's new teacher is limited to that same section (CreateTeacherUseCase).
        // The class-master class, when given, still has to be in the caller's section.
        @SectionNeutral
        @PostMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and (#param.classMaster() == null or #param.classMaster().isBlank() or @sectionScope.classSession(#school, #param.classMaster()))")
        public ApiResponse<TeacherDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody CreateTeacherDTO param) {
                var title = Title.valueOf(param.title());
                var gender = Gender.valueOf(param.gender());

                boolean sendWelcomeEmail = param.sendWelcomeEmail() == null || param.sendWelcomeEmail();
                boolean teaching = param.teaching() == null || param.teaching();

                var res = createTeacherUseCase.execute(school, title, param.givenNames(), param.familyName(), gender,
                                param.staffId(),
                                param.email(), param.phone(), param.street(), param.city(), param.classMaster(),
                                param.designation(), sendWelcomeEmail, teaching);
                return new ApiResponse<>("success", 200,
                                "Teacher created successfully. Password is generated automatically.", res);
        }

        @SectionScoped
        @PatchMapping("/edit/{id}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.teacher(#school, #id)")
        public ApiResponse<TeacherDTO> edit(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestBody EditTeacherDTO param,
                        @PathVariable(required = true) String id) {
                var title = Title.valueOf(param.title());
                var gender = Gender.valueOf(param.gender());

                var payload = new TeacherRecord(school, id, title, param.givenNames(), param.familyName(), gender,
                                param.staffId(), param.phone(), param.street(), param.city(), param.designation());
                var res = teacherSvc.edit(payload);
                return new ApiResponse<>("success", 200,
                                "Teacher edited successfully.", res);
        }

        @SectionScoped
        @PatchMapping("/{id}/status")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.teacher(#school, #id)")
        public ApiResponse<TeacherDTO> setStatus(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @AuthenticationPrincipal(expression = "userId") String userId,
                        @PathVariable String id,
                        @RequestParam("active") boolean active) {
                var res = teacherSvc.setStatus(school, id, userId, active);
                String message = active ? "Staff reactivated successfully" : "Staff deactivated successfully";
                return new ApiResponse<>("success", 200, message, res);
        }

        @SectionNeutral
        @GetMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
        public ApiResponse<List<TeacherDTO>> listBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String gender,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String direction) {

                if (search == null || search.isBlank()) {
                        search = null;
                }

                var res = teacherSvc.search(search, gender, page - 1, size, school, sortBy, direction);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Teachers fetched successfully", list, meta);
        }

        @SectionScoped
        @GetMapping("/{id}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.teacher(#school, #id)")
        public ApiResponse<TeacherDTO> listById(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(required = true) String id,
                        @RequestParam(required = false) String academicYearId) {

                var res = teacherSvc.getById(id, academicYearId);
                return new ApiResponse<>("success", 200, "Teacher fetched successfully", res);
        }

        @SectionScoped
        @GetMapping("/subject")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.clazz(#school, #classId)")
        public ApiResponse<List<TeacherSubjectDTO>> listSubjectBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = false) String academicYearId,
                        @RequestParam(required = false) String classId,
                        @RequestParam(required = false) String streamId,
                        @RequestParam(required = false) String query,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String direction,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {
                var res = listTeacherSubjectBySchoolUseCase.execute(school, academicYearId, classId, streamId, query,
                                page - 1, size, sortBy, direction);
                var list = res.getContent();
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());

                return new ApiResponse<>("success", 200, "Teacher subjects fetched successfully", list,
                                meta);
        }

        @SectionScoped
        @GetMapping("/subject/{teacherId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.teacher(#school, #teacherId)")
        public ApiResponse<List<TeacherSubjectDTO>> listSubjectByTeacher(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(required = true) String teacherId) {
                var list = listTeacherSubjectByTeacherUseCase.execute(school, teacherId);
                return new ApiResponse<>("success", 200, "Teacher subjects fetched successfully", list);
        }

        @SectionScoped
        @GetMapping("/subject/me")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
        public ApiResponse<List<TeacherSubjectDTO>> listSubjectByMe(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @AuthenticationPrincipal(expression = "userId") String userId) {
                var list = listTeacherSubjectByTeacherUseCase.executeByUser(school, userId);
                return new ApiResponse<>("success", 200, "Teacher subjects fetched successfully", list);
        }

        @SectionScoped
        @GetMapping("/class-master/me")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
        public ApiResponse<List<TeacherClassMasterDTO>> listMyClassMasterAssignments(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @AuthenticationPrincipal(expression = "userId") String userId) {
                var list = listMyClassMasterAssignmentsUseCase.execute(school, userId);
                return new ApiResponse<>("success", 200, "Class master assignments fetched successfully", list);
        }

        @SectionScoped
        @GetMapping("/subject/session/{sessionId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.classSession(#school, #sessionId)")
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

        @SectionScoped
        @GetMapping("/subject/detail/{teacherId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.teacher(#school, #teacherId)")
        public ApiResponse<TeacherSubjectDTO> oneSubjectByTeacher(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String teacherId) {
                var res = getTeacherSubjectUseCase.execute(school, teacherId);
                return new ApiResponse<>("success", 200, "Teacher subject fetched successfully", res);
        }
}
