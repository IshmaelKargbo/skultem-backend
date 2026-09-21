package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.security.RequiresModule;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.MaterialCategoryDTO;
import com.moriba.skultem.application.dto.MaterialDTO;
import com.moriba.skultem.application.dto.MaterialTrasactionDTO;
import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.application.services.MaterialService;
import com.moriba.skultem.domain.model.Material.Unit;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateMaterialCategoryDTO;
import com.moriba.skultem.infrastructure.rest.dto.CreateMaterialDTO;
import com.moriba.skultem.infrastructure.rest.dto.RestockMaterialDTO;
import com.moriba.skultem.infrastructure.rest.dto.SupplyMaterialDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateMaterialCategoryDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateMaterialDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RequiresModule(FeatureModule.MATERIALS_AND_SUPPLIES)
@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService service;

    @PostMapping("/category")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialCategoryDTO> createCategory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateMaterialCategoryDTO param) {
        var res = service.createMaterialCategory(school, param.name(), param.description());
        return new ApiResponse<>("success", 200, "Material category successfully", res);
    }

    @PutMapping("/category/{categoryId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialCategoryDTO> updateCategory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String categoryId,
            @Valid @RequestBody UpdateMaterialCategoryDTO param) {
        var res = service.updateMaterialCategory(school, categoryId, param.name(), param.description());
        return new ApiResponse<>("success", 200, "Material category updated successfully", res);
    }

    @DeleteMapping("/category/{categoryId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<Object> deleteCategory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String categoryId) {
        service.deleteMaterialCategory(school, categoryId);
        return new ApiResponse<>("success", 200, "Material category deleted successfully", null);
    }

    @PostMapping()
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialDTO> createMaterial(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateMaterialDTO param) {
        var res = service.createMaterial(school, param.name(), Unit.valueOf(param.unit()), param.inStock(),
                param.price(), param.categoryId());
        return new ApiResponse<>("success", 200, "Material category successfully", res);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialDTO> updateMaterial(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateMaterialDTO param) {
        var res = service.updateMaterial(school, id, param.name(), Unit.valueOf(param.unit()), param.price(),
                param.categoryId());
        return new ApiResponse<>("success", 200, "Material updated successfully", res);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<Object> deleteMaterial(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        service.deleteMaterial(school, id);
        return new ApiResponse<>("success", 200, "Material deleted successfully", null);
    }

    @PostMapping("/restock")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<MaterialDTO> restockMaterial(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody RestockMaterialDTO param) {
        var res = service.restockMaterial(school, param.id(), param.inStock(), param.note());
        return new ApiResponse<>("success", 200, "Material restock successfully", res);
    }

    @PostMapping("/supply")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'ACCOUNTANT')")
    public ApiResponse<SupplyDTO> supplyMaterial(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody SupplyMaterialDTO param) {
        var res = service.supplyMaterial(school, param.id(), param.qty(), param.note());
        return new ApiResponse<>("success", 200, "Material supply successfully", res);
    }

    @GetMapping("/category")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<MaterialCategoryDTO>> listCategory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = false) String search) {

        var res = service.listCategory(school, page, size, search);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Material category fetched successfully", list, meta);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<MaterialDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = false) String search) {

        var res = service.listMaterial(school, page, size, search);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Material fetched successfully", list, meta);
    }

    @GetMapping("/supply")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<SupplyDTO>> listSupply(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = false) String search) {

        var res = service.listSupply(school, page, size, search);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Supply fetched successfully", list, meta);
    }

    @GetMapping("/{id}/transaction")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<MaterialTrasactionDTO>> listTransaction(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = true) String id,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = service.listMaterialTransaction(school, id, page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Material transaction fetched successfully", list, meta);
    }

}
