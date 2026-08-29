package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.application.dto.LessonDTO;
import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.application.dto.SchemeProgressDTO;
import com.moriba.skultem.application.dto.TeacherProgressDTO;
import com.moriba.skultem.application.dto.TeacherProgressDetailDTO;
import com.moriba.skultem.application.dto.WeekDTO;
import com.moriba.skultem.application.services.CurriculumService;
import com.moriba.skultem.infrastructure.rest.dto.*;
import com.moriba.skultem.infrastructure.rest.mapper.MetaMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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