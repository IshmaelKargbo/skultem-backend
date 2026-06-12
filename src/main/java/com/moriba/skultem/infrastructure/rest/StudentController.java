package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.dto.ActiveCycleDTO;
import com.moriba.skultem.application.dto.ParentRequest;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.dto.StudentFinanceOverviewDTO;
import com.moriba.skultem.application.dto.StudentRecord;
import com.moriba.skultem.application.services.StudentService;
import com.moriba.skultem.application.usecase.ActiveCycleUseCase;
import com.moriba.skultem.application.usecase.CreateStudentUseCase;
import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.vo.Family;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.application.usecase.GetStudentFinanceOverviewUseCase;
import com.moriba.skultem.application.usecase.GetStudentUseCase;
import com.moriba.skultem.application.usecase.ListSubjectFeesByStudentUseCase;
import com.moriba.skultem.application.usecase.RankStudentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateStudentDTO;
import com.moriba.skultem.infrastructure.rest.mapper.MetaMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {
        private final CreateStudentUseCase createStudentUseCase;
        private final GetStudentUseCase getStudentUseCase;
        private final GetStudentFinanceOverviewUseCase getStudentFinanceOverviewUseCase;
        private final ListSubjectFeesByStudentUseCase listSubjectFeesByStudentUseCase;
        private final RankStudentUseCase rankStudentUseCase;
        private final StudentService studentSvc;
        private final ActiveCycleUseCase activeCycleUseCase;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
        public ApiResponse<StudentDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestPart("data") CreateStudentDTO param,
                        @RequestPart(value = "photo", required = false) MultipartFile photo) {
                var args = validateCreateStudentRequest(param, school, photo);
                var res = createStudentUseCase.execute(args);
                return new ApiResponse<>("success", 200, "Student created successfully", res);
        }

        @GetMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
        public ApiResponse<List<StudentDTO>> listBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                if (search == null || search.isBlank()) {
                        search = null;
                }

                var res = studentSvc.search(search, page, size, school);
                var list = res.getContent();
                var meta = MetaMapper.toMeta(res);

                return new ApiResponse<>("success", 200, "Students fetched successfully", list, meta);
        }

        @GetMapping("/rank/{studentId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT')")
        public ApiResponse<Object> rankStudent(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(required = true) String studentId,
                        @RequestParam(required = true) String termId) {
                var res = rankStudentUseCase.execute(studentId, termId, school);
                return new ApiResponse<>("success", 200, "Student rank fetched successfully", res);
        }

        @GetMapping("/cycle/{sessionId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT')")
        public ApiResponse<ActiveCycleDTO> activeCycle(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(name = "sessionId") String sessionId) {
                var res = activeCycleUseCase.execute(school, sessionId);
                return new ApiResponse<>("success", 200, "Active cycle fetch successfully", res);
        }

        @GetMapping("/fee/{studentId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'ACCOUNTANT')")
        public ApiResponse<List<StudentFeeDTO>> listStudentFees(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String studentId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = listSubjectFeesByStudentUseCase.execute(school, studentId, page - 1, size);
                var list = res.getContent();
                var meta = MetaMapper.toMeta(res);

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
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'ACCOUNTANT')")
        public ApiResponse<StudentFinanceOverviewDTO> financeOverview(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @RequestParam(required = false, defaultValue = "10") Integer recentPayments) {
                var size = Math.max(1, recentPayments);
                var res = getStudentFinanceOverviewUseCase.execute(school, id, size);
                return new ApiResponse<>("success", 200, "Student finance overview fetched successfully", res);
        }

        private StudentRecord validateCreateStudentRequest(CreateStudentDTO param, String school, MultipartFile photo) {
                if (param.parent() == null && (param.parentId() == null || param.parentId().isBlank())) {
                        throw new IllegalArgumentException("Parent is required");
                }

                ParentRequest parent = param.parent() != null
                                ? new ParentRequest(school, param.parent().givenNames(), param.parent().familyName(),
                                                param.parent().email(), param.parent().phone(), param.parent().street(),
                                                param.parent().city())
                                : null;
                Family family = new Family(param.family().fatherName(), param.family().motherName(),
                                param.family().fatherOccupation(), param.family().motherOccupation(),
                                param.family().motherContact(),
                                param.family().fatherContact());
                Gender gender = Gender.valueOf(param.gender());
                EnrollmentType enrollmentType = param.enrollmentType() != null
                                ? EnrollmentType.valueOf(param.enrollmentType())
                                : EnrollmentType.NEW;

                return new StudentRecord(param.classId(), school, photo, param.givenNames(), param.familyName(),
                                param.admissionNumber(), param.admissionDate(), enrollmentType, param.previousSchool(),
                                param.nationality(), param.religion(), param.city(), param.street(),
                                param.lastClass(), param.house(), gender, family, param.parent().relationship(),
                                param.parentId(), parent, param.dateOfBirth(), param.selectedOptionIds());
        }
}
