package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.StreamDTO;
import com.moriba.skultem.application.dto.StreamSubjectDTO;
import com.moriba.skultem.application.usecase.CreateStreamUseCase;
import com.moriba.skultem.application.usecase.GetStreamUseCase;
import com.moriba.skultem.application.usecase.ListStreamBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListStreamSubjectBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListStreamSubjectByStreamUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateStreamDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamController {
    private final CreateStreamUseCase createStreamUseCase;
    private final ListStreamBySchoolUseCase listStreamBySchoolUseCase;
    private final ListStreamSubjectBySchoolUseCase listStreamSubjectBySchoolUseCase;
    private final ListStreamSubjectByStreamUseCase listStreamSubjectByStreamUseCase;
    private final GetStreamUseCase getStreamUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR')")
    public ApiResponse<StreamDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody CreateStreamDTO param) {
        var res = createStreamUseCase.execute(school, param.name(), param.description());
        return new ApiResponse<>("success", 200, "Stream created successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<StreamDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listStreamBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Streams fetched successfully", list, meta);
    }

    @GetMapping("/subject")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<StreamSubjectDTO>> listSubjects(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listStreamSubjectBySchoolUseCase.execute(school, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Stream subjects fetched successfully", list,
                meta);
    }

    @GetMapping("/subject/{streamId}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<StreamSubjectDTO>> listSubjectsByStreamId(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable(required = true) String streamId,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {
        var res = listStreamSubjectByStreamUseCase.execute(school, streamId, page - 1, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Stream subjects fetched successfully", list,
                meta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<StreamDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getStreamUseCase.execute(id, school);
        return new ApiResponse<>("success", 200, "Stream fetched successfully", res);
    }
}
