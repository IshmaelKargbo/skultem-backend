package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionScoped;

import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.security.RequiresModule;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
import com.moriba.skultem.application.dto.ReportCardAssessmentOptionDTO;
import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardStatsDTO;
import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.usecase.GenerateReportCardsUseCase;
import com.moriba.skultem.application.usecase.GetReportCardStatsUseCase;
import com.moriba.skultem.application.usecase.GetReportCardUseCase;
import com.moriba.skultem.application.usecase.ListReportCardAssessmentsUseCase;
import com.moriba.skultem.application.usecase.ListReportCardsUseCase;
import com.moriba.skultem.application.usecase.ListStudentReportCardsUseCase;
import com.moriba.skultem.application.usecase.RenderReportCardPdfUseCase;
import com.moriba.skultem.application.usecase.TrackReportCardDownloadUseCase;
import com.moriba.skultem.application.usecase.UpdateReportCardRemarkUseCase;
import com.moriba.skultem.infrastructure.report.ReportFile;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.GenerateReportCardsRequestDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateReportCardRemarkDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiresModule(FeatureModule.REPORT_CARDS)
@RestController
@RequestMapping("/api/v1/report-card")
@RequiredArgsConstructor
public class ReportCardController {

    private final GenerateReportCardsUseCase generateReportCardsUseCase;
    private final ListReportCardsUseCase listReportCardsUseCase;
    private final ListStudentReportCardsUseCase listStudentReportCardsUseCase;
    private final GetReportCardUseCase getReportCardUseCase;
    private final GetReportCardStatsUseCase getReportCardStatsUseCase;
    private final UpdateReportCardRemarkUseCase updateReportCardRemarkUseCase;
    private final TrackReportCardDownloadUseCase trackReportCardDownloadUseCase;
    private final ListReportCardAssessmentsUseCase listReportCardAssessmentsUseCase;
    private final com.moriba.skultem.application.usecase.ReviewReportCardsUseCase reviewReportCardsUseCase;
    private final com.moriba.skultem.infrastructure.security.PermissionService permissionService;
    private final RenderReportCardPdfUseCase renderReportCardPdfUseCase;
    private final com.moriba.skultem.domain.repository.SchoolRepository schoolRepo;
    private final com.moriba.skultem.application.services.SchoolBrandingResolver brandingResolver;

    // ---- Review: the class teacher approves a class's generated cards (then parents see them), or sends them back
    // to the admin with a note. ids narrows it to some students; empty = all of the class's cards for the term.

    @SectionScoped
    @PostMapping("/review/approve")
    @PreAuthorize("@permissionService.canReviewReportCardsForClass(#school, #param.classId()) and @sectionScope.clazz(#school, #param.classId())")
    public ApiResponse<com.moriba.skultem.application.usecase.ReviewReportCardsUseCase.Result> approve(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody com.moriba.skultem.infrastructure.rest.dto.ReviewReportCardsDTO param) {
        var res = reviewReportCardsUseCase.approve(school, param.classId(), param.termId(), param.ids());
        return new ApiResponse<>("success", 200, "Report cards approved", res);
    }

    @SectionScoped
    @PostMapping("/review/return")
    @PreAuthorize("@permissionService.canReviewReportCardsForClass(#school, #param.classId()) and @sectionScope.clazz(#school, #param.classId())")
    public ApiResponse<com.moriba.skultem.application.usecase.ReviewReportCardsUseCase.Result> sendBack(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @Valid @RequestBody com.moriba.skultem.infrastructure.rest.dto.ReviewReportCardsDTO param) {
        var res = reviewReportCardsUseCase.sendBack(school, param.classId(), param.termId(), param.ids(), param.note());
        return new ApiResponse<>("success", 200, "Report cards sent back to the admin", res);
    }

    @SectionScoped
    // Only admins generate; the class teacher then reviews and approves (see /review).
    @PostMapping("/generate")
    @PreAuthorize("@permissionService.canGenerateReportCards(#school) and @sectionScope.clazz(#school, #param.classId())")
    public ApiResponse<GenerateReportCardsResultDTO> generate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody GenerateReportCardsRequestDTO param) {
        var dto = new GenerateReportCardsDTO(param.classId(), param.termId(), param.includeAttendance(),
                param.includeRanking(), param.assessmentIds(), param.wholeYear(),
                param.sectionId(), param.streamId(), param.academicYearId());
        var res = generateReportCardsUseCase.execute(school, userId, dto);
        return new ApiResponse<>("success", 200, "Report cards generated successfully", res);
    }

    @SectionScoped
    // A class master has to name one of their own classes; management can list across the school.
    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR') or @permissionService.canManageReportCardsForClass(#school, #classId)")
    public ApiResponse<List<ReportCardSummaryDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String sectionId,
            @RequestParam(required = false) String streamId,
            @RequestParam(required = true, defaultValue = "1") Integer page,
            @RequestParam(required = true, defaultValue = "12") Integer size) {
        var res = listReportCardsUseCase.execute(school, classId, termId, search, level, sectionId, streamId, page,
                size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Report cards fetched successfully", list, meta);
    }

    // A parent sees their own child's report cards this way; #studentId is otherwise unscoped to
    // the caller, so isParentOfStudent is what actually stops one family from browsing another's
    // by guessing/enumerating a studentId.
    @SectionScoped
    @GetMapping("/student/{studentId}")
    @PreAuthorize("(@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR') or @permissionService.isParentOfStudent(#school, #studentId)) and @sectionScope.student(#school, #studentId)")
    public ApiResponse<List<ReportCardSummaryDTO>> byStudent(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId) {
        // Staff see every card; a parent only the approved ones.
        boolean staff = permissionService.hasAnySchoolRole(school, "ADMIN", "OWNER", "PRINCIPAL", "SUPER_ADMIN",
                "PROPRIETOR");
        var res = listStudentReportCardsUseCase.execute(school, studentId, !staff);
        return new ApiResponse<>("success", 200, "Report cards fetched successfully", res);
    }

    // The assessments a class's report cards can be limited to (the options on the Generate page).
    @SectionScoped
    @GetMapping("/assessments")
    @PreAuthorize("@permissionService.canGenerateReportCards(#school) and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<List<ReportCardAssessmentOptionDTO>> assessments(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classId) {
        var res = listReportCardAssessmentsUseCase.execute(school, classId);
        return new ApiResponse<>("success", 200, "Assessments fetched successfully", res);
    }

    @com.moriba.skultem.infrastructure.security.SectionNeutral
    @GetMapping("/stats")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR')")
    public ApiResponse<ReportCardStatsDTO> stats(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        var res = getReportCardStatsUseCase.execute(school);
        return new ApiResponse<>("success", 200, "Report card stats fetched successfully", res);
    }

    // ---- PDFs made by M-Compose (see RenderReportCardPdfUseCase). GET /m-compose/status says whether they are on;
    // when they are not, the app draws the PDF in the browser instead.

    // A whole class's cards for a term, one student after another, in a single PDF to print.
    @SectionScoped
    @GetMapping("/pdf")
    @PreAuthorize("(@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PRINCIPAL', 'SUPER_ADMIN', 'PROPRIETOR') or @permissionService.canManageReportCardsForClass(#school, #classId)) and @sectionScope.clazz(#school, #classId)")
    public ResponseEntity<byte[]> classPdf(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String classId,
            @RequestParam String termId,
            @RequestParam(required = false) String scope) {
        return pdf(renderReportCardPdfUseCase.forClass(school, classId, termId, scope));
    }

    // The school's report card design as it prints (Report Cards > Design): its latest card, or placeholders.
    // template: a design to try before choosing it.
    @com.moriba.skultem.infrastructure.security.SectionNeutral
    @GetMapping("/preview")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'PRINCIPAL', 'EXAM_OFFICER', 'SUPER_ADMIN', 'OWNER', 'PROPRIETOR')")
    public ResponseEntity<byte[]> designPreview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = false) String template) {
        var schoolDto = schoolRepo.findById(school).map(s -> brandingResolver.schoolDtoFor(s, null)).orElse(null);
        return inlinePdf(renderReportCardPdfUseCase.preview(school, template, schoolDto));
    }

    // One card, for whoever may open it (staff, or the student's parent).
    @SectionScoped
    @GetMapping("/{id}/pdf")
    @PreAuthorize("(@permissionService.canManageReportCard(#school, #id) or @permissionService.isParentOfReportCard(#school, #id)) and @sectionScope.reportCard(#school, #id)")
    public ResponseEntity<byte[]> pdf(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean inline) {
        var file = renderReportCardPdfUseCase.single(school, id);
        return inline ? inlinePdf(file) : pdf(file);
    }

    // Shown in the page (to look at before printing) rather than saved.
    private static ResponseEntity<byte[]> inlinePdf(ReportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.data());
    }

    private static ResponseEntity<byte[]> pdf(ReportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.data());
    }

    @SectionScoped
    @GetMapping("/{id}")
    @PreAuthorize("(@permissionService.canManageReportCard(#school, #id) or @permissionService.isParentOfReportCard(#school, #id)) and @sectionScope.reportCard(#school, #id)")
    public ApiResponse<ReportCardDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getReportCardUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Report card fetched successfully", res);
    }

    @SectionScoped
    // The class teacher's comment, while the card is under review.
    @PatchMapping("/{id}/remark")
    @PreAuthorize("@permissionService.canReviewReportCard(#school, #id) and @sectionScope.reportCard(#school, #id)")
    public ApiResponse<ReportCardDTO> updateRemark(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestBody UpdateReportCardRemarkDTO param) {
        var res = updateReportCardRemarkUseCase.execute(school, id, param.remark());
        return new ApiResponse<>("success", 200, "Report card remark updated successfully", res);
    }

    @SectionScoped
    @PostMapping("/{id}/download")
    @PreAuthorize("(@permissionService.canManageReportCard(#school, #id) or @permissionService.isParentOfReportCard(#school, #id)) and @sectionScope.reportCard(#school, #id)")
    public ApiResponse<Void> trackDownload(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        trackReportCardDownloadUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Download tracked successfully", null);
    }
}
