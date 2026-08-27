package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import com.moriba.skultem.application.services.SchoolService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.OwnerDTO;
import com.moriba.skultem.application.dto.SchoolBrandingAssetsDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.usecase.CreateSchoolUseCase;
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
    private final SchoolService schoolSvc;

    @PostMapping
    public ApiResponse<SchoolDTO> create(@Valid @RequestBody CreateSchoolDTO param) {
        var owner = new OwnerDTO(param.givenNames(), param.familyName(), param.email(), param.password(),
                param.phone());
        var address = new Address(param.region(), param.district(), param.chiefdom(), param.city(), param.street());
        var res = createSchoolUseCase.execute(param.name(), param.domain(), address, owner);
        return new ApiResponse<>("success", 200, "School created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<List<SchoolDTO>> list(@RequestParam(required = true, defaultValue = "10") Integer size,
                                             @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listSchoolUseCase.execute(page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<List<SchoolDTO>>("success", 200, "Schools fetched successfully", list, meta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<SchoolDTO> get(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = schoolSvc.get(school);
        return new ApiResponse<>("success", 200, "School fetched successfully", res);
    }

    // Logo/signature as inline data: URIs - see GetSchoolBrandingAssetsUseCase
    // for why (R2's public bucket sends no CORS headers, which breaks the ID
    // card's PDF export when it tries to draw those images onto a canvas).
    @GetMapping("/branding/assets")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT', 'TEACHER', 'PARENT')")
    public ApiResponse<SchoolBrandingAssetsDTO> getBrandingAssets(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getSchoolBrandingAssetsUseCase.execute(school);
        return new ApiResponse<>("success", 200, "School branding assets fetched successfully", res);
    }

    @PutMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SchoolDTO> update(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody UpdateSchoolDTO param) {
        var address = new Address(param.region(), param.district(), param.chiefdom(), param.city(), param.street());
        var res = updateSchoolUseCase.execute(school, param.name(), param.domain(), address);
        return new ApiResponse<>("success", 200, "School updated successfully", res);
    }

    @PutMapping(value = "/branding", consumes = "multipart/form-data")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<SchoolDTO> updateBranding(@AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String principalName,
            @RequestParam(required = false) String primaryColor,
            @RequestParam(required = false) String secondaryColor,
            @RequestPart(required = false) MultipartFile logo,
            @RequestPart(required = false) MultipartFile principalSignature) {
        var res = updateSchoolBrandingUseCase.execute(school, principalName, logo, principalSignature, primaryColor,
                secondaryColor);
        return new ApiResponse<>("success", 200, "School branding updated successfully", res);
    }
}
