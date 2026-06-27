package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
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

    @GetMapping("/scheme/{sessionId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<SchemeOfWorkDTO>> roomSearch(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @PathVariable(required = false) String sessionId) {
        var res = curriculumSvc.search(page, size, sessionId);
        Map<String, Object> meta = MetaMapper.toMeta(res);
        return new ApiResponse<>("success", 200, "Scheme of work fetched successfully", res.getContent(), meta);
    }
}