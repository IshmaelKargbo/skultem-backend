package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AcademicYearDTO;
import com.moriba.skultem.application.dto.ConfigureNextAcademicYearResultDTO;
import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.AcademicService;
import com.moriba.skultem.application.usecase.ActiveAcademicYearUseCase;
import com.moriba.skultem.application.usecase.AssignNextAcademicYearUseCase;
import com.moriba.skultem.application.usecase.ConfigureNextAcademicYearUseCase;
import com.moriba.skultem.application.usecase.CreateAcadamicYearUseCase;
import com.moriba.skultem.application.usecase.DeleteAcademicYearUseCase;
import com.moriba.skultem.application.usecase.GetAcademicYearUseCase;
import com.moriba.skultem.application.usecase.ListAcademicYearUseCase;
import com.moriba.skultem.application.usecase.UpdateAcademicYearUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.AssignNextAcademicYearDTO;
import com.moriba.skultem.infrastructure.rest.dto.ConfigureNextAcademicYearDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateAcademicYearDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping({"/api/v1/academic_year", "/api/v1/academic-year"})
@RequiredArgsConstructor
public class AcademicYearController {

    private final CreateAcadamicYearUseCase createAcadamicYearUseCase;
    private final ListAcademicYearUseCase listAcademicYearUseCase;
    private final GetAcademicYearUseCase getAcademicYearUseCase;
    private final ActiveAcademicYearUseCase activeAcademicYearUseCase;
    private final ConfigureNextAcademicYearUseCase configureNextAcademicYearUseCase;
    private final AssignNextAcademicYearUseCase assignNextAcademicYearUseCase;
    private final UpdateAcademicYearUseCase updateAcademicYearUseCase;
    private final DeleteAcademicYearUseCase deleteAcademicYearUseCase;
    private final AcademicService academicSvc;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<AcademicYearDTO> createAcademicYear(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateAcademicYearDTO param) {
        if (param.endDate().isBefore(param.startDate())) {
            throw new RuleException("End date must be after start date");
        }
        var res = createAcadamicYearUseCase.execute(school, param.name(), param.startDate(), param.endDate());
        return new ApiResponse<>("success", 200, "Academic year created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER', 'TEACHER')")
    public ApiResponse<List<AcademicYearDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listAcademicYearUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Academic year fetched successfully", list, meta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<AcademicYearDTO> setActiveAcademicYearBySchool(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = activeAcademicYearUseCase.execute(id);
        return new ApiResponse<>("success", 200, "Academic year set active successfully", res);
    }

    @PostMapping("/{id}/next")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<ConfigureNextAcademicYearResultDTO> configureNext(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestBody(required = false) ConfigureNextAcademicYearDTO param) {
        var body = param != null ? param : new ConfigureNextAcademicYearDTO(null, null, null);
        if (body.endDate() != null && body.startDate() != null && body.endDate().isBefore(body.startDate())) {
            throw new RuleException("End date must be after start date");
        }
        var res = configureNextAcademicYearUseCase.execute(school, id, body.name(), body.startDate(), body.endDate());
        return new ApiResponse<>("success", 200, "Next academic year configured successfully", res);
    }

    @PutMapping("/{id}/next")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'OWNER')")
    public ApiResponse<AcademicYearDTO> assignNext(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody AssignNextAcademicYearDTO param) {
        var res = assignNextAcademicYearUseCase.execute(school, id, param.nextYearId());
        return new ApiResponse<>("success", 200, "Next academic year assigned successfully", res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canAccessSchool(#school)")
    public ApiResponse<AcademicYearDTO> get(
            @PathVariable String id,
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getAcademicYearUseCase.execute(id);
        return new ApiResponse<>("success", 200, "Academic year fetched successfully", res);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<AcademicYearDTO> update(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody CreateAcademicYearDTO param) {
        var res = updateAcademicYearUseCase.execute(school, id, param.name(), param.startDate(), param.endDate());
        return new ApiResponse<>("success", 200, "Academic year updated successfully", res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        deleteAcademicYearUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Academic year deleted successfully", null);
    }

    @GetMapping("/terms")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'PARENT', 'TEACHER')")
    public ApiResponse<List<TermDTO>> listByAcademicYear(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String academicYearId) {
        var res = academicSvc.getAcademicTerms(school, academicYearId);
        return new ApiResponse<>("success", 200, "Terms fetched successfully", res);
    }
}
