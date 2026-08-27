package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.GenerateReportCardsDTO;
import com.moriba.skultem.application.dto.GenerateReportCardsResultDTO;
import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardStatsDTO;
import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.usecase.GenerateReportCardsUseCase;
import com.moriba.skultem.application.usecase.GetReportCardStatsUseCase;
import com.moriba.skultem.application.usecase.GetReportCardUseCase;
import com.moriba.skultem.application.usecase.ListReportCardsUseCase;
import com.moriba.skultem.application.usecase.TrackReportCardDownloadUseCase;
import com.moriba.skultem.application.usecase.UpdateReportCardRemarkUseCase;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.GenerateReportCardsRequestDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateReportCardRemarkDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/report-card")
@RequiredArgsConstructor
public class ReportCardController {

    private final GenerateReportCardsUseCase generateReportCardsUseCase;
    private final ListReportCardsUseCase listReportCardsUseCase;
    private final GetReportCardUseCase getReportCardUseCase;
    private final GetReportCardStatsUseCase getReportCardStatsUseCase;
    private final UpdateReportCardRemarkUseCase updateReportCardRemarkUseCase;
    private final TrackReportCardDownloadUseCase trackReportCardDownloadUseCase;

    @PostMapping("/generate")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<GenerateReportCardsResultDTO> generate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody GenerateReportCardsRequestDTO param) {
        var dto = new GenerateReportCardsDTO(param.classId(), param.termId(), param.includeAttendance(),
                param.includeRanking());
        var res = generateReportCardsUseCase.execute(school, userId, dto);
        return new ApiResponse<>("success", 200, "Report cards generated successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<List<ReportCardSummaryDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String search,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = true, defaultValue = "12") Integer size) {
        var res = listReportCardsUseCase.execute(school, classId, termId, search, page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Report cards fetched successfully", list, meta);
    }

    @GetMapping("/stats")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ReportCardStatsDTO> stats(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getReportCardStatsUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Report card stats fetched successfully", res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ReportCardDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getReportCardUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Report card fetched successfully", res);
    }

    @PatchMapping("/{id}/remark")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<ReportCardDTO> updateRemark(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestBody UpdateReportCardRemarkDTO param) {
        var res = updateReportCardRemarkUseCase.execute(school, id, param.remark());
        return new ApiResponse<>("success", 200, "Report card remark updated successfully", res);
    }

    @PostMapping("/{id}/download")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<Void> trackDownload(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        trackReportCardDownloadUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Download tracked successfully", null);
    }
}
