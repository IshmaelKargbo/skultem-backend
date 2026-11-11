package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.ExternalExamDTO;
import com.moriba.skultem.application.services.DocumentDesignService;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.application.usecase.ExternalExamUseCase;
import com.moriba.skultem.application.usecase.RenderExternalExamDocumentUseCase;
import com.moriba.skultem.application.usecase.RenderExternalExamDocumentUseCase.Kind;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.report.ReportFile;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.RequiresModule;
import com.moriba.skultem.infrastructure.security.SectionNeutral;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import lombok.RequiredArgsConstructor;

/**
 * External exams (NPSE, BECE, WASSCE, ...): the exams and their grade scales, each exam class's candidates and
 * scores, confirming them, and printing result slips and statements of result through M-Compose.
 * The exam office runs it (SUPER_ADMIN passes any gate naming EXAM_OFFICER).
 */
@RequiresModule(FeatureModule.EXTERNAL_EXAMS)
@RestController
@RequestMapping("/api/v1/external-exams")
@RequiredArgsConstructor
public class ExternalExamController {

    private static final String MANAGE = "@permissionService.hasAnySchoolRole(#school, 'EXAM_OFFICER', 'ADMIN', 'PRINCIPAL', 'OWNER', 'PROPRIETOR')";
    private static final String SITTING = MANAGE + " and @sectionScope.externalExamSitting(#school, #id)";

    private final ExternalExamUseCase useCase;
    private final RenderExternalExamDocumentUseCase documents;
    private final SectionScopeService sectionScopeService;
    private final DocumentDesignService designs;

    // ---- Exams and their grade scales

    @SectionNeutral
    @GetMapping("/exams")
    @PreAuthorize(MANAGE)
    public ApiResponse<List<ExternalExamDTO.Exam>> exams(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return ok("Exams fetched", useCase.listExams(school));
    }

    @SectionNeutral
    @PostMapping("/exams")
    @PreAuthorize(MANAGE)
    public ApiResponse<ExternalExamDTO.Exam> createExam(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestBody ExternalExamDTO.SaveExam body) {
        return ok("Exam added", useCase.createExam(school, body));
    }

    @SectionNeutral
    @PutMapping("/exams/{examId}")
    @PreAuthorize(MANAGE)
    public ApiResponse<ExternalExamDTO.Exam> updateExam(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String examId, @RequestBody ExternalExamDTO.SaveExam body) {
        return ok("Exam saved", useCase.updateExam(school, examId, body));
    }

    @SectionNeutral
    @DeleteMapping("/exams/{examId}")
    @PreAuthorize(MANAGE)
    public ApiResponse<Void> deleteExam(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String examId) {
        useCase.deleteExam(school, examId);
        return ok("Exam deleted", null);
    }

    // ---- Print designs: one general design per document for every exam (/document-designs/exam-result-slip and
    // exam-statement), and any exam's own on top. kind: exam-result-slip | exam-statement.

    @SectionNeutral
    @GetMapping("/exams/{examId}/designs/{kind}")
    @PreAuthorize(MANAGE)
    public ApiResponse<DocumentDesignService.Choice> examDesign(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String examId,
            @PathVariable String kind, @RequestParam(defaultValue = "false") boolean all) {
        var exam = useCase.exam(school, examId);
        return ok("Designs fetched", designs.examChoice(school, exam, examKind(kind), all));
    }

    // template: a design's ref; empty = use the school's general design for this exam.
    @SectionNeutral
    @PutMapping("/exams/{examId}/designs/{kind}")
    @PreAuthorize(MANAGE)
    public ApiResponse<DocumentDesignService.Choice> setExamDesign(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String examId,
            @PathVariable String kind, @RequestBody DocumentDesignController.ChooseDesign body) {
        var docKind = examKind(kind);
        String template = body == null || body.template() == null ? null : body.template().trim();
        if (template != null && !template.isEmpty()) {
            designs.requireDesign(template);
        }
        var exam = useCase.setExamDesign(school, examId, docKind == DocumentKind.EXAM_STATEMENT, template);
        return ok("Design saved", designs.examChoice(school, exam, docKind, true));
    }

    private static DocumentKind examKind(String key) {
        return DocumentKind.fromKey(key).filter(k -> k != DocumentKind.REPORT_CARD)
                .orElseThrow(() -> new com.moriba.skultem.application.error.NotFoundException(
                        "Unknown exam document: " + key));
    }

    // ---- Exam classes (sittings)

    @SectionScoped
    @GetMapping("/class-subjects")
    @PreAuthorize(MANAGE + " and @sectionScope.clazz(#school, #classId)")
    public ApiResponse<List<ExternalExamDTO.SubjectOption>> classSubjects(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @RequestParam String classId) {
        return ok("Subjects fetched", useCase.classSubjects(school, classId));
    }

    @SectionScoped
    @GetMapping("/sittings")
    @PreAuthorize(MANAGE)
    public ApiResponse<List<ExternalExamDTO.SittingSummary>> sittings(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return ok("Exam classes fetched", useCase.listSittings(school, sectionScopeService.restrictedLevels()));
    }

    @SectionScoped
    @PostMapping("/sittings")
    @PreAuthorize(MANAGE + " and @sectionScope.clazz(#school, #body.classId())")
    public ApiResponse<ExternalExamDTO.Sitting> createSitting(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestBody ExternalExamDTO.CreateSitting body) {
        return ok("Exam class added", useCase.createSitting(school, body));
    }

    @SectionScoped
    @GetMapping("/sittings/{id}")
    @PreAuthorize(SITTING)
    public ApiResponse<ExternalExamDTO.Sitting> sitting(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id) {
        return ok("Exam results fetched", useCase.sitting(school, id));
    }

    @SectionScoped
    @PutMapping("/sittings/{id}")
    @PreAuthorize(SITTING)
    public ApiResponse<ExternalExamDTO.Sitting> updateSitting(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id,
            @RequestBody ExternalExamDTO.UpdateSitting body) {
        return ok("Exam class saved", useCase.updateSitting(school, id, body));
    }

    @SectionScoped
    @DeleteMapping("/sittings/{id}")
    @PreAuthorize(SITTING)
    public ApiResponse<Void> deleteSitting(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id) {
        useCase.deleteSitting(school, id);
        return ok("Exam class deleted", null);
    }

    @SectionScoped
    @PostMapping("/sittings/{id}/candidates/refresh")
    @PreAuthorize(SITTING)
    public ApiResponse<ExternalExamDTO.Sitting> refresh(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id) {
        return ok("Candidates updated from the class", useCase.refreshCandidates(school, id));
    }

    @SectionScoped
    @DeleteMapping("/sittings/{id}/candidates/{candidateId}")
    @PreAuthorize(SITTING)
    public ApiResponse<ExternalExamDTO.Sitting> removeCandidate(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id,
            @PathVariable String candidateId) {
        return ok("Candidate removed", useCase.removeCandidate(school, id, candidateId));
    }

    @SectionScoped
    @PutMapping("/sittings/{id}/scores")
    @PreAuthorize(SITTING)
    public ApiResponse<ExternalExamDTO.Sitting> saveScores(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id,
            @RequestBody ExternalExamDTO.SaveScores body) {
        return ok("Scores saved", useCase.saveScores(school, id, body));
    }

    @SectionScoped
    @PostMapping("/sittings/{id}/confirm")
    @PreAuthorize(SITTING)
    public ApiResponse<ExternalExamDTO.Sitting> confirm(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId, @PathVariable String id) {
        return ok("Results confirmed", useCase.confirm(school, id, userId));
    }

    @SectionScoped
    @PostMapping("/sittings/{id}/reopen")
    @PreAuthorize(SITTING)
    public ApiResponse<ExternalExamDTO.Sitting> reopen(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id) {
        return ok("Results reopened for changes", useCase.reopen(school, id));
    }

    // ---- Documents (M-Compose). inline=true shows it in the browser (to view before printing).

    @SectionScoped
    @GetMapping("/sittings/{id}/document")
    @PreAuthorize(SITTING)
    public ResponseEntity<byte[]> allDocuments(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id,
            @RequestParam(defaultValue = "slip") String type, @RequestParam(defaultValue = "false") boolean inline) {
        return pdf(documents.all(school, id, Kind.of(type)), inline);
    }

    @SectionScoped
    @GetMapping("/sittings/{id}/candidates/{candidateId}/document")
    @PreAuthorize(SITTING)
    public ResponseEntity<byte[]> candidateDocument(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school, @PathVariable String id,
            @PathVariable String candidateId, @RequestParam(defaultValue = "slip") String type,
            @RequestParam(defaultValue = "false") boolean inline) {
        return pdf(documents.candidate(school, id, candidateId, Kind.of(type)), inline);
    }

    private static ResponseEntity<byte[]> pdf(ReportFile file, boolean inline) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (inline ? "inline" : "attachment") + "; filename=\"" + file.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.data());
    }

    private static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("success", 200, message, data);
    }
}
