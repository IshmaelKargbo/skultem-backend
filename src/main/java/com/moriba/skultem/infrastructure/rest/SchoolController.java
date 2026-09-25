package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionNeutral;
import com.moriba.skultem.infrastructure.security.SectionScoped;
import com.moriba.skultem.application.usecase.UpdateSectionBrandingUseCase;
import com.moriba.skultem.domain.vo.Level;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

import com.moriba.skultem.application.services.SchoolService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.AssetDataUriDTO;
import com.moriba.skultem.application.dto.OwnerDTO;
import com.moriba.skultem.application.dto.SchoolBrandingAssetsDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.usecase.CreateSchoolUseCase;
import com.moriba.skultem.application.usecase.GetSchoolStructureUseCase;
import com.moriba.skultem.application.usecase.UpdateSchoolStructureUseCase;
import com.moriba.skultem.application.dto.SchoolStructureDTO;
import com.moriba.skultem.infrastructure.rest.dto.SchoolStructureRequestDTO;
import com.moriba.skultem.application.usecase.GetAssetDataUriUseCase;
import com.moriba.skultem.application.usecase.GetSchoolBrandingAssetsUseCase;
import com.moriba.skultem.application.usecase.ListSchoolUseCase;
import com.moriba.skultem.application.usecase.UpdateSchoolBrandingUseCase;
import com.moriba.skultem.application.usecase.UpdateSchoolUseCase;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateSchoolDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateSchoolDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class SchoolController {

    private final CreateSchoolUseCase createSchoolUseCase;
    private final ListSchoolUseCase listSchoolUseCase;
    private final UpdateSchoolUseCase updateSchoolUseCase;
    private final UpdateSchoolBrandingUseCase updateSchoolBrandingUseCase;
    private final GetSchoolBrandingAssetsUseCase getSchoolBrandingAssetsUseCase;
    private final GetAssetDataUriUseCase getAssetDataUriUseCase;
    private final SchoolService schoolSvc;
    private final GetSchoolStructureUseCase getSchoolStructureUseCase;
    private final UpdateSchoolStructureUseCase updateSchoolStructureUseCase;
    private final UpdateSectionBrandingUseCase updateSectionBrandingUseCase;

    @PostMapping
    public ApiResponse<SchoolDTO> create(@Valid @RequestBody CreateSchoolDTO param) {
        var owner = new OwnerDTO(param.givenNames(), param.familyName(), param.email(), param.password(),
                param.phone());
        var address = new Address(param.region(), param.district(), param.chiefdom(), param.city(), param.street());
        var structure = param.structure();
        var res = structure == null
                ? createSchoolUseCase.execute(param.name(), param.domain(), address, owner, null, null, null)
                : createSchoolUseCase.execute(param.name(), param.domain(), address, owner,
                        structure.managementModel(), structure.levels(), toSectionInputs(structure));
        return new ApiResponse<>("success", 200, "School created successfully", res);
    }

    // Public (no auth) - powers the "Trusted by N schools" line on the login page, which is
    // rendered before anyone has signed in. /api/v1/school/** is already open at the filter-chain
    // level (SecurityConfig) for the signup flow above; this is deliberately the one endpoint
    // under it with no @PreAuthorize, since it exposes nothing but a count.
    @GetMapping("/count")
    public ApiResponse<Map<String, Long>> count() {
        return new ApiResponse<>("success", 200, "School count fetched successfully",
                Map.of("count", schoolSvc.countAll()));
    }

    @GetMapping
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<List<SchoolDTO>> list(@RequestParam(required = true, defaultValue = "10") Integer size,
                                             @RequestParam(required = true, defaultValue = "1") Integer page,
                                             @RequestParam(required = false) String query) {
        var res = listSchoolUseCase.execute(page - 1, size, query);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<List<SchoolDTO>>("success", 200, "Schools fetched successfully", list, meta);
    }

    // Read by anything that lists or picks levels (class forms, filters) - so every staff role.
    @SectionNeutral
    @GetMapping("/structure")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<SchoolStructureDTO> structure(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getSchoolStructureUseCase.execute(school);
        return new ApiResponse<>("success", 200, "School structure fetched successfully", res);
    }

    // Owner-level only: the structure decides management scope, so a plain ADMIN (who may later be
    // scoped to one section) must not be able to redraw it.
    @PutMapping("/structure")
    @PreAuthorize("@permissionService.isSchoolLeadership(#school)")
    public ApiResponse<SchoolStructureDTO> updateStructure(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SchoolStructureRequestDTO param) {
        var res = updateSchoolStructureUseCase.execute(school, param.managementModel(), param.levels(),
                toSectionInputs(param));
        return new ApiResponse<>("success", 200, "School structure updated successfully", res);
    }

    // One management section's own logo / principal / signature / location. Owner-level may edit
    // any section; a section-limited Admin only the sections they're limited to.
    @SectionScoped
    @PutMapping(value = "/structure/sections/{id}/branding", consumes = "multipart/form-data")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.managementSection(#id)")
    public ApiResponse<SchoolStructureDTO> updateSectionBranding(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestParam(required = false) String principalName,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String chiefdom,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String street,
            @RequestParam(required = false, defaultValue = "false") boolean removeLogo,
            @RequestParam(required = false, defaultValue = "false") boolean removeSignature,
            @RequestPart(required = false) MultipartFile logo,
            @RequestPart(required = false) MultipartFile principalSignature) {
        var input = new UpdateSectionBrandingUseCase.Input(principalName,
                new Address(region, district, chiefdom, city, street), logo, principalSignature, removeLogo,
                removeSignature);
        var res = updateSectionBrandingUseCase.execute(school, id, input);
        return new ApiResponse<>("success", 200, "Section branding updated successfully", res);
    }

    private static List<UpdateSchoolStructureUseCase.SectionInput> toSectionInputs(SchoolStructureRequestDTO param) {
        if (param.sections() == null) {
            return List.of();
        }
        return param.sections().stream()
                .map(s -> new UpdateSchoolStructureUseCase.SectionInput(s.id(), s.name(), s.levels()))
                .toList();
    }

    @SectionNeutral
    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<SchoolDTO> get(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = schoolSvc.get(school);
        return new ApiResponse<>("success", 200, "School fetched successfully", res);
    }

    // Logo/signature as inline data: URIs - see GetSchoolBrandingAssetsUseCase
    // for why (R2's public bucket sends no CORS headers, which breaks the ID
    // card's PDF export when it tries to draw those images onto a canvas).
    @SectionNeutral
    @GetMapping("/branding/assets")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<SchoolBrandingAssetsDTO> getBrandingAssets(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false, defaultValue = "true") boolean inline) {
        var res = getSchoolBrandingAssetsUseCase.execute(school,
                new GetSchoolBrandingAssetsUseCase.Target(level, studentId, teacherId, referenceNo), inline);
        return new ApiResponse<>("success", 200, "School branding assets fetched successfully", res);
    }

    // Same idea as branding/assets above but for any one R2 asset URL - used for a student/staff
    // photo on the ID card, which needs the same same-origin swap right before PDF/print capture.
    @SectionNeutral
    @GetMapping("/asset-as-data-uri")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<AssetDataUriDTO> getAssetAsDataUri(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String url) {
        var res = getAssetDataUriUseCase.execute(url);
        return new ApiResponse<>("success", 200, "Asset fetched successfully", res);
    }

    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SchoolDTO> update(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody UpdateSchoolDTO param) {
        var address = new Address(param.region(), param.district(), param.chiefdom(), param.city(), param.street());
        var res = updateSchoolUseCase.execute(school, param.name(), param.domain(), address,
                param.attendanceThreshold(), param.genderComposition());
        return new ApiResponse<>("success", 200, "School updated successfully", res);
    }

    @PutMapping(value = "/branding", consumes = "multipart/form-data")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SchoolDTO> updateBranding(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String motto,
            @RequestParam(required = false) String principalName,
            @RequestParam(required = false) String primaryColor,
            @RequestParam(required = false) String secondaryColor,
            @RequestPart(required = false) MultipartFile logo,
            @RequestPart(required = false) MultipartFile principalSignature) {
        var res = updateSchoolBrandingUseCase.execute(school, motto, principalName, logo, principalSignature,
                primaryColor, secondaryColor);
        return new ApiResponse<>("success", 200, "School branding updated successfully", res);
    }
}
