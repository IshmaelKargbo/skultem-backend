package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.idempotency.Idempotent;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import java.io.IOException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.BulkImportStudentsUseCase;
import com.moriba.skultem.application.dto.BulkStudentImportResultDTO;
import com.moriba.skultem.application.services.StudentService;
import com.moriba.skultem.application.usecase.ActiveCycleUseCase;
import com.moriba.skultem.application.usecase.CreateStudentUseCase;
import com.moriba.skultem.application.usecase.UpdateStudentPhotoUseCase;
import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.vo.Family;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.application.usecase.GetStudentFinanceOverviewUseCase;
import com.moriba.skultem.application.usecase.GetStudentUseCase;
import com.moriba.skultem.application.usecase.DeleteStudentPermanentlyUseCase;
import com.moriba.skultem.application.usecase.EndStudentEnrollmentUseCase;
import com.moriba.skultem.application.usecase.ReinstateStudentUseCase;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.infrastructure.rest.dto.DeleteStudentDTO;
import com.moriba.skultem.infrastructure.rest.dto.EndStudentEnrollmentDTO;
import com.moriba.skultem.application.usecase.ListSubjectFeesByStudentUseCase;
import com.moriba.skultem.application.usecase.RankStudentUseCase;
import com.moriba.skultem.application.usecase.ReprocessStudentPhotosUseCase;
import com.moriba.skultem.application.usecase.UpdateStudentUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateStudentDTO;
import com.moriba.skultem.infrastructure.rest.dto.EditStudentDTO;
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
        private final UpdateStudentPhotoUseCase updateStudentPhotoUseCase;
        private final ReprocessStudentPhotosUseCase reprocessStudentPhotosUseCase;
        private final UpdateStudentUseCase updateStudentUseCase;
        private final BulkImportStudentsUseCase bulkImportStudentsUseCase;
        private final EndStudentEnrollmentUseCase endStudentEnrollmentUseCase;
        private final ReinstateStudentUseCase reinstateStudentUseCase;
        private final DeleteStudentPermanentlyUseCase deleteStudentPermanentlyUseCase;

        @Idempotent(operation = "student.enroll")
        @SectionScoped
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.classSession(#school, #param.classId())")
        public ApiResponse<StudentDTO> create(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @Valid @RequestPart("data") CreateStudentDTO param,
                        @RequestPart(value = "photo", required = false) MultipartFile photo) {
                var args = validateCreateStudentRequest(param, school, photo);
                var res = createStudentUseCase.execute(args);
                return new ApiResponse<>("success", 200, "Student created successfully", res);
        }

        // Section-limited admins can import, but only into their own section's classes - enforced
        // row by row in BulkImportStudentsUseCase, since the classes come from the file.
        @SectionScoped
        @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
        public ApiResponse<BulkStudentImportResultDTO> bulkImport(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestPart("file") MultipartFile file,
                        @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) {
                if (file.isEmpty()) {
                        throw new RuleException("Choose a CSV file to upload.");
                }
                try {
                        var res = bulkImportStudentsUseCase.execute(school, file.getInputStream(), dryRun);
                        String message = dryRun
                                        ? res.ready() + " student(s) ready to import"
                                                        + (res.failed() > 0 ? ", " + res.failed() + " row(s) need fixing" : "")
                                        : "Imported " + res.created() + " student(s)"
                                                        + (res.failed() > 0 ? ", " + res.failed() + " failed" : "");
                        return new ApiResponse<>("success", 200, message, res);
                } catch (IOException e) {
                        throw new RuleException("Could not read the uploaded file.");
                }
        }

        @SectionScoped
        @PatchMapping("/edit/{id}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.student(#school, #id)")
        public ApiResponse<StudentDTO> edit(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @Valid @RequestBody EditStudentDTO param) {
                Gender gender = Gender.valueOf(param.gender());
                var res = updateStudentUseCase.execute(school, id, param.admissionNumber(), param.givenNames(),
                                param.familyName(), gender, param.dateOfBirth(), param.nationality(),
                                param.religion(), param.city(), param.street());
                return new ApiResponse<>("success", 200, "Student edited successfully", res);
        }

        @SectionScoped
        @PostMapping("/{id}/withdraw")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.student(#school, #id)")
        public ApiResponse<StudentDTO> withdraw(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @Valid @RequestBody EndStudentEnrollmentDTO param) {
                var res = endStudentEnrollmentUseCase.execute(school, id, Student.Status.WITHDRAWN, param.reason(),
                                param.exitDate(), param.note());
                return new ApiResponse<>("success", 200, "Student withdrawn - their enrollment has been stopped", res);
        }

        @SectionScoped
        @PostMapping("/{id}/expel")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.student(#school, #id)")
        public ApiResponse<StudentDTO> expel(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @Valid @RequestBody EndStudentEnrollmentDTO param) {
                var res = endStudentEnrollmentUseCase.execute(school, id, Student.Status.EXPELLED, param.reason(),
                                param.exitDate(), param.note());
                return new ApiResponse<>("success", 200, "Student expelled", res);
        }

        @SectionScoped
        @PostMapping("/{id}/reinstate")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.student(#school, #id)")
        public ApiResponse<StudentDTO> reinstate(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id) {
                var res = reinstateStudentUseCase.execute(school, id);
                return new ApiResponse<>("success", 200, "Student reinstated", res);
        }

        // Owner-level only: this can't be undone. The admission number in the body is the confirmation.
        @SectionScoped
        @PostMapping("/{id}/delete-permanently")
        @PreAuthorize("@permissionService.isSchoolLeadership(#school) and @sectionScope.student(#school, #id)")
        public ApiResponse<DeleteStudentPermanentlyUseCase.Result> deletePermanently(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @Valid @RequestBody DeleteStudentDTO param) {
                var res = deleteStudentPermanentlyUseCase.execute(school, id, param.confirmation());
                return new ApiResponse<>("success", 200, res.studentName() + " was permanently deleted", res);
        }

        @SectionScoped
        @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.student(#school, #id)")
        public ApiResponse<StudentDTO> updatePhoto(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @RequestPart("photo") MultipartFile photo) {
                var res = updateStudentPhotoUseCase.execute(school, id, photo);
                return new ApiResponse<>("success", 200, "Student photo updated successfully", res);
        }

        @PostMapping("/photos/reprocess")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
        public ApiResponse<ReprocessStudentPhotosUseCase.Summary> reprocessPhotos(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
                var res = reprocessStudentPhotosUseCase.execute(school);
                return new ApiResponse<>("success", 200, "Student photos reprocessed successfully", res);
        }

        @SectionScoped
        @GetMapping
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
        public ApiResponse<List<StudentDTO>> listBySchool(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String academicYearId,
                        @RequestParam(required = false) String classId,
                        @RequestParam(required = false) Gender gender,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String direction,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                if (search == null || search.isBlank()) {
                        search = null;
                }

                var res = studentSvc.search(search, page, size, school, academicYearId, classId, sortBy, direction, gender);
                var list = res.getContent();
                var meta = MetaMapper.toMeta(res);

                return new ApiResponse<>("success", 200, "Students fetched successfully", list, meta);
        }

        @SectionScoped
        @GetMapping("/rank/{studentId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT') and @sectionScope.student(#school, #studentId)")
        public ApiResponse<Object> rankStudent(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(required = true) String studentId,
                        @RequestParam(required = true) String termId) {
                var res = rankStudentUseCase.execute(studentId, termId, school);
                return new ApiResponse<>("success", 200, "Student rank fetched successfully", res);
        }

        @SectionScoped
        @GetMapping("/cycle/{sessionId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT') and @sectionScope.classSession(#school, #sessionId)")
        public ApiResponse<ActiveCycleDTO> activeCycle(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable(name = "sessionId") String sessionId,
                        @RequestParam(required = false) String academicYearId) {
                var res = activeCycleUseCase.execute(school, sessionId, academicYearId);
                return new ApiResponse<>("success", 200, "Active cycle fetch successfully", res);
        }

        @SectionScoped
        @GetMapping("/fee/{studentId}")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'ACCOUNTANT', 'PARENT') and @sectionScope.student(#school, #studentId)")
        public ApiResponse<List<StudentFeeDTO>> listStudentFees(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String studentId,
                        @RequestParam(required = false) String academicYearId,
                        @RequestParam(required = true, defaultValue = "10") Integer size,
                        @RequestParam(required = true, defaultValue = "1") Integer page) {

                var res = listSubjectFeesByStudentUseCase.execute(school, studentId, academicYearId, page - 1, size);
                var list = res.getContent();
                var meta = MetaMapper.toMeta(res);

                return new ApiResponse<>("success", 200, "Student fees fetched successfully", list, meta);
        }

        @SectionScoped
        @GetMapping("/{id}")
        @PreAuthorize("@permissionService.canAccessSchool(#school) and @sectionScope.student(#school, #id)")
        public ApiResponse<StudentDTO> get(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @RequestParam(required = false) String academicYearId) {
                var res = getStudentUseCase.execute(id, school, academicYearId);
                return new ApiResponse<>("success", 200, "Student fetched successfully", res);
        }

        @SectionScoped
        @GetMapping("/{id}/finance-overview")
        @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'ACCOUNTANT') and @sectionScope.student(#school, #id)")
        public ApiResponse<StudentFinanceOverviewDTO> financeOverview(
                        @AuthenticationPrincipal(expression = "activeSchoolId") String school,
                        @PathVariable String id,
                        @RequestParam(required = false) String academicYearId,
                        @RequestParam(required = false, defaultValue = "10") Integer recentPayments) {
                var size = Math.max(1, recentPayments);
                var res = getStudentFinanceOverviewUseCase.execute(school, id, academicYearId, size);
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
