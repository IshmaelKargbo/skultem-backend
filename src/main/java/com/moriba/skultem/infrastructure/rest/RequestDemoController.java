package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import com.moriba.skultem.application.services.RequestDemoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.RequestDemoDTO;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateRequestDemoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/demos")
@RequiredArgsConstructor
public class RequestDemoController {

    private final RequestDemoService demoSvc;

    @PostMapping
    public ApiResponse<RequestDemoDTO> create(@Valid @RequestBody CreateRequestDemoDTO param) {
        var res = demoSvc.create(param.name(), param.email(), param.school(), param.phone(), param.city(),
                param.address(), param.preferred(), param.priority(), param.message());
        return new ApiResponse<>("success", 200, "Request demo created successfully", res);
    }

    @GetMapping
    public ApiResponse<List<RequestDemoDTO>> list(@RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = demoSvc.list(page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<List<RequestDemoDTO>>("success", 200, "Request demo fetched successfully", list, meta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'SYSTEM_ADMIN')")
    public ApiResponse<RequestDemoDTO> get(@PathVariable(required = true) String school) {
        var res = demoSvc.get(school);
        return new ApiResponse<>("success", 200, "School fetched successfully", res);
    }
}
