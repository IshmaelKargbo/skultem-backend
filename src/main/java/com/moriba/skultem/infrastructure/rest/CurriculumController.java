package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.security.RequiresModule;
import com.moriba.skultem.application.dto.BulkSchemeOfWorkResultDTO;
import com.moriba.skultem.application.dto.ChildSchemeOfWorkDTO;
import com.moriba.skultem.application.dto.LessonDTO;
import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.application.dto.SchemeProgressDTO;
import com.moriba.skultem.application.dto.TeacherProgressDTO;
import com.moriba.skultem.application.dto.TeacherProgressDetailDTO;
import com.moriba.skultem.application.dto.WeekDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.CurriculumService;
import com.moriba.skultem.infrastructure.rest.dto.*;
import com.moriba.skultem.infrastructure.rest.mapper.MetaMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiresModule(FeatureModule.CURRICULUM)
@RestController
@RequestMapping("/api/v1/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumSvc;

    @PostMapping("/scheme")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<SchemeOfWorkDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateSchemeOfWorkDTO param) {
        var res = curriculumSvc.create(school, param.session(), param.term(), param.subject());
        return new ApiResponse<>("success", 200, "Scheme of work created successfully", res);
    }

    // CSV upload with a "class,subject,term,week,topic,subTopic,objectives" header - each row is
    // one week; objectives is optional and "|"-separated for more than one. Rows sharing the same
    // class/subject/term reuse (or create once) the same scheme. Names are
    // matched against the school's current active academic year (see BulkCreateSchemeOfWorkUseCase
    // for how an ambiguous class name, e.g. one with multiple sections/streams, is resolved). Each
    // row succeeds or fails independently, so a typo in one row doesn't block the rest of the file.
    @PostMapping(value = "/scheme/bulk", consumes = "multipart/form-data")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<BulkSchemeOfWorkResultDTO> bulkCreate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuleException("Choose a CSV file to upload.");
        }

        try {
            var res = curriculumSvc.bulkCreate(school, file.getInputStream());
            String message = res.failed() == 0
                    ? "Created " + res.created() + " week(s)" + (res.skipped() > 0 ? ", " + res.skipped() + " already existed" : "")
                    : "Created " + res.created() + ", skipped " + res.skipped() + ", " + res.failed() + " failed - see details";
            return new ApiResponse<>("success", 200, message, res);
        } catch (IOException e) {
            throw new RuleException("Could not read the uploaded file.");
        }
    }

    @PostMapping("/scheme/week")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<WeekDTO> createWeek(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateWeekDTO param) {
        var res = curriculumSvc.createWeek(school, param.scheme(), param.week(), param.topic(), param.subtopic(), param.objectives());
        return new ApiResponse<>("success", 200, "Week created successfully", res);
    }

    @GetMapping("/scheme")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<SchemeOfWorkDTO>> searchSchemaOfWork(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String progress) {
        var res = curriculumSvc.searchScheme(page, size, school, subjectId, sessionId, termId, progress);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Scheme of work fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/scheme/me")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<SchemeOfWorkDTO>> searchMySchemaOfWork(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String progress) {
        var res = curriculumSvc.searchMyScheme(school, userId, page, size, subjectId, sessionId, termId, progress);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Scheme of work fetched successfully", res.getContent(), meta);
    }

    // Self-service for a parent - their own child's published scheme(s) of work, resolved from
    // the child's class session server-side (see GetChildCurriculumUseCase) rather than trusting
    // a client-supplied sessionId, and filtered to PUBLISH only.
    @GetMapping("/scheme/child/{studentId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'PARENT')")
    public ApiResponse<List<ChildSchemeOfWorkDTO>> getChildCurriculum(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String studentId,
            @RequestParam(required = false) String termId) {
        var res = curriculumSvc.getChildCurriculum(school, userId, studentId, termId);
        return new ApiResponse<>("success", 200, "Curriculum fetched successfully", res);
    }

    @PatchMapping("/scheme/{id}/state")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<SchemeOfWorkDTO> updateSchemeState(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateSchemeStateDTO param) {
        var res = curriculumSvc.updateSchemeState(school, id, param.state());
        String message = "PUBLISH".equals(param.state()) ? "Scheme of work published successfully"
                : "Scheme of work moved back to draft";
        return new ApiResponse<>("success", 200, message, res);
    }

    @GetMapping("/scheme/one/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<SchemeOfWorkDTO> getSchemaOfWork(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = false) String id) {
        var res = curriculumSvc.getScheme(id);
        return new ApiResponse<>("success", 200, "Scheme of work fetched successfully", res);
    }

    @GetMapping("/scheme/progress/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<SchemeProgressDTO> getSchemeProgress(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = false) String id) {
        var res = curriculumSvc.getSchemeProgress(id);
        return new ApiResponse<>("success", 200, "Scheme progress fetched successfully", res);
    }

    @GetMapping("/session/scheme/weeks")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<WeekDTO>> getSchoolSchemeWeeks(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = curriculumSvc.getWeeksByAcademicYear(school, academicYearId, page, size);
        var data = res.getContent();
        var meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Scheme weeks fetched successfully", data, meta);
    }

    @GetMapping("/scheme/weeks/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<WeekDTO>> getSchemeWeeks(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = false) String id) {
        var res = curriculumSvc.getWeeks(id);
        return new ApiResponse<>("success", 200, "Scheme weeks fetched successfully", res);
    }

    @GetMapping("/scheme/week/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<WeekDTO> getWeek(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = curriculumSvc.getWeek(id);
        return new ApiResponse<>("success", 200, "Week fetched successfully", res);
    }

    @PatchMapping("/scheme/week/{id}/state")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<WeekDTO> updateWeekState(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateWeekStateDTO param) {
        var res = curriculumSvc.updateWeekState(school, id, param.state());
        return new ApiResponse<>("success", 200, "Week updated successfully", res);
    }

    @PostMapping("/scheme/week/lesson")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<LessonDTO> createLesson(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateLessonDTO param) {
        var res = curriculumSvc.createLesson(
                school,
                param.week(),
                param.title(),
                param.content(),
                param.date(),
                param.duration(),
                param.objectives(),
                param.previousKnowledge(),
                param.teachingAids(),
                param.referenceMaterials(),
                param.presentation(),
                param.evaluation(),
                param.assignment());
        return new ApiResponse<>("success", 200, "Lesson note created successfully", res);
    }

    @GetMapping("/scheme/week/{weekId}/lessons")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<LessonDTO>> getWeekLessons(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String weekId) {
        var res = curriculumSvc.getLessons(weekId);
        return new ApiResponse<>("success", 200, "Lesson notes fetched successfully", res);
    }

    @GetMapping("/lesson/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<LessonDTO> getLesson(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = curriculumSvc.getLesson(id);
        return new ApiResponse<>("success", 200, "Lesson note fetched successfully", res);
    }

    @PatchMapping("/lesson/{id}/state")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<LessonDTO> updateLessonState(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateLessonStateDTO param) {
        var res = curriculumSvc.updateLessonState(school, id, param.state());
        return new ApiResponse<>("success", 200, "Lesson note updated successfully", res);
    }

    @GetMapping("/lesson/me")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<LessonDTO>> searchMyLessons(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page) {
        var res = curriculumSvc.searchMyLessons(school, userId, page, size);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Lesson notes fetched successfully", res.getContent(), meta);
    }

    // Self-service - a teacher's own coverage, for the "My Progress" card on their own
    // /curriculums page (Spring matches this literal "/me" segment ahead of the
    // /teacher-progress/{teacherId} path variable below regardless of declaration order).
    @GetMapping("/teacher-progress/me")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<TeacherProgressDetailDTO> getMyTeacherProgress(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId) {
        var res = curriculumSvc.getMyTeacherProgress(school, userId);
        return new ApiResponse<>("success", 200, "Teacher progress fetched successfully", res);
    }

    @GetMapping("/teacher-progress")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<TeacherProgressDTO>> getTeacherProgress(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = curriculumSvc.getTeacherProgress(school);
        return new ApiResponse<>("success", 200, "Teacher progress fetched successfully", res);
    }

    @GetMapping("/teacher-progress/{teacherId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<TeacherProgressDetailDTO> getTeacherProgressDetail(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String teacherId) {
        var res = curriculumSvc.getTeacherProgress(school, teacherId);
        return new ApiResponse<>("success", 200, "Teacher progress fetched successfully", res);
    }
}