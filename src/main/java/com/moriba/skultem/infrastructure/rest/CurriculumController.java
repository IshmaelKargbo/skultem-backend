package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.application.dto.SchemeProgressDTO;
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
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SchemeOfWorkDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateSchemeOfWorkDTO param) {
        var res = curriculumSvc.create(school, param.session(), param.term(), param.subject());
        return new ApiResponse<>("success", 200, "Scheme of work created successfully", res);
    }

    @PostMapping("/scheme/week")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
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
            @RequestParam(defaultValue = "1") Integer page) {
        var res = curriculumSvc.searchScheme(page, size, school);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Scheme of work fetched successfully", res.getContent(), meta);
    }

    @GetMapping("/scheme/one/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SchemeOfWorkDTO> getSchemaOfWork(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = false) String id) {
        var res = curriculumSvc.getScheme(id);
        return new ApiResponse<>("success", 200, "Scheme of work fetched successfully", res);
    }

    @GetMapping("/scheme/progress/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SchemeProgressDTO> getSchemeProgress(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = false) String id) {
        var res = curriculumSvc.getSchemeProgress(id);
        return new ApiResponse<>("success", 200, "Scheme progress fetched successfully", res);
    }

    @GetMapping("/scheme/weeks/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<WeekDTO>> getSchemeWeeks(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = false) String id) {
        var res = curriculumSvc.getWeeks(id);
        return new ApiResponse<>("success", 200, "Scheme weeks fetched successfully", res);
    }
}