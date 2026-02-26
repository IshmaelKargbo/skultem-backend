package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.OwnerDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.usecase.CreateSchoolUseCase;
import com.moriba.skultem.application.usecase.GetSchoolUseCase;
import com.moriba.skultem.application.usecase.ListSchoolUseCase;
import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateSchoolDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class SchoolController {

    private final CreateSchoolUseCase createSchoolUseCase;
    private final ListSchoolUseCase listSchoolUseCase;
    private final GetSchoolUseCase getSchoolUseCase;

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
    @PreAuthorize("@permissionService.isSystemAdmin()")
    public ApiResponse<SchoolDTO> get(@PathVariable String id) {
        var res = getSchoolUseCase.execute(id);
        return new ApiResponse<SchoolDTO>("success", 200, "School fetched successfully", res);
    }
}
