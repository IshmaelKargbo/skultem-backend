package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
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

import com.moriba.skultem.application.dto.HealthCareReportDTOs.Check;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.FrequentVisitor;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Limits;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.MedicationCheck;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Overview;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Periods;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Trends;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.StartResult;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.StudentHistory;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Summary;
import com.moriba.skultem.application.dto.HealthCareSettingDTO;
import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.services.HealthCareService;
import com.moriba.skultem.application.usecase.AddHealthCareMedicationUseCase;
import com.moriba.skultem.application.usecase.AddHealthCareNoteUseCase;
import com.moriba.skultem.application.usecase.CloseHealthCareVisitUseCase;
import com.moriba.skultem.application.usecase.NotifyHealthCareParentsUseCase;
import com.moriba.skultem.application.usecase.RemoveHealthCareMedicationUseCase;
import com.moriba.skultem.application.usecase.StartHealthCareVisitUseCase;
import com.moriba.skultem.application.usecase.UpdateHealthCareSettingUseCase;
import com.moriba.skultem.application.usecase.UpdateHealthCareVisitUseCase;
import com.moriba.skultem.application.usecase.VoidHealthCareVisitUseCase;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository.Filter;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.idempotency.Idempotent;
import com.moriba.skultem.infrastructure.report.HealthCareExporter;
import com.moriba.skultem.infrastructure.report.ReportFile;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.HealthCareRequests;
import com.moriba.skultem.infrastructure.security.RequiresModule;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Health Care (docs/HEALTH_CARE.md): sick bay visits, the school's health care rules, and reports.
 * <p>
 * Who may do what (§4): the nurse records visits; principal-level roles see everything, add follow-up notes, void,
 * follow up visits nobody closed, and are the only ones who change the rules. A plain ADMIN, teachers and
 * accountants get nothing here - medical details stay with the nurse and principal-level (Q-2, Q-3).
 * SUPER_ADMIN passes any gate naming NURSE (PermissionService.SUPER_ADMIN_COVERS).
 */
@RequiresModule(FeatureModule.HEALTH_CARE)
@RestController
@RequestMapping("/api/v1/health-care")
@RequiredArgsConstructor
public class HealthCareController {

    private static final String RECORD = "@permissionService.hasAnySchoolRole(#school, 'NURSE')";
    private static final String VIEW = "@permissionService.hasAnySchoolRole(#school, 'NURSE', 'PRINCIPAL', 'OWNER', 'PROPRIETOR', 'SUPER_ADMIN')";
    private static final String MANAGE_SETTINGS = "@permissionService.hasAnySchoolRole(#school, 'PRINCIPAL', 'OWNER', 'PROPRIETOR', 'SUPER_ADMIN')";

    private final HealthCareService service;
    private final HealthCareExporter exporter;
    private final StartHealthCareVisitUseCase startUseCase;
    private final UpdateHealthCareVisitUseCase updateUseCase;
    private final AddHealthCareMedicationUseCase addMedicationUseCase;
    private final RemoveHealthCareMedicationUseCase removeMedicationUseCase;
    private final CloseHealthCareVisitUseCase closeUseCase;
    private final AddHealthCareNoteUseCase noteUseCase;
    private final VoidHealthCareVisitUseCase voidUseCase;
    private final NotifyHealthCareParentsUseCase notifyUseCase;
    private final UpdateHealthCareSettingUseCase settingUseCase;

    @GetMapping("/overview")
    @PreAuthorize(VIEW)
    public ApiResponse<Overview> overview(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return ok("Health care overview fetched successfully", service.overview(school));
    }

    // Visits

    @GetMapping("/visits")
    @PreAuthorize(VIEW)
    public ApiResponse<List<HealthCareVisitDTO>> listVisits(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String complaint,
            @RequestParam(defaultValue = "false") boolean overLimit,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String search) {
        return paged(service.listVisits(school, page, size, from, to, filter(status), outcome(outcome), complaint,
                overLimit, studentId, search), "Health care visits fetched successfully");
    }

    @GetMapping("/visits/{id}")
    @PreAuthorize(VIEW)
    public ApiResponse<HealthCareVisitDTO> visit(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        return ok("Health care visit fetched successfully", service.visit(school, id));
    }

    @GetMapping("/check")
    @PreAuthorize(RECORD)
    public ApiResponse<Check> check(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String studentId,
            @RequestParam(required = false) String complaints) {
        List<String> codes = complaints == null || complaints.isBlank() ? List.of()
                : Arrays.stream(complaints.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return ok("Checked", service.check(school, studentId, codes));
    }

    @PostMapping("/visits")
    @PreAuthorize(RECORD)
    @Idempotent(operation = "health-care.visit.start")
    public ApiResponse<StartResult> start(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody HealthCareRequests.StartVisit p) {
        var res = startUseCase.execute(school, new StartHealthCareVisitUseCase.Command(p.studentId(), p.arrivedAt(),
                p.complaints(), p.complaintOther(), p.broughtBy(), p.temperature(), p.notes(), p.continuesVisitId(),
                p.overLimitReason()), userId);
        return ok(res.turnedAway() ? "Visit not started: the limit was reached" : "Visit started", res);
    }

    @PutMapping("/visits/{id}")
    @PreAuthorize(RECORD)
    public ApiResponse<HealthCareVisitDTO> update(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody HealthCareRequests.UpdateVisit p) {
        return ok("Visit updated", updateUseCase.execute(school, id, new UpdateHealthCareVisitUseCase.Command(
                p.complaints(), p.complaintOther(), p.broughtBy(), p.temperature(), p.notes(), p.treatment()), userId));
    }

    @PostMapping("/visits/{id}/medications")
    @PreAuthorize(RECORD)
    public ApiResponse<HealthCareVisitDTO> addMedication(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody HealthCareRequests.AddMedication p) {
        return ok("Medication recorded", addMedicationUseCase.execute(school, id,
                new AddHealthCareMedicationUseCase.Command(p.name(), p.dose(), p.givenAt(), p.repeatReason()), userId));
    }

    @DeleteMapping("/visits/{id}/medications/{medicationId}")
    @PreAuthorize(RECORD)
    public ApiResponse<HealthCareVisitDTO> removeMedication(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @PathVariable String medicationId) {
        return ok("Medication line removed", removeMedicationUseCase.execute(school, id, medicationId, userId));
    }

    @GetMapping("/visits/{id}/medication-check")
    @PreAuthorize(RECORD)
    public ApiResponse<MedicationCheck> medicationCheck(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @RequestParam String name) {
        return ok("Checked", service.medicationCheck(school, id, name));
    }

    @PostMapping("/visits/{id}/close")
    @PreAuthorize(RECORD)
    public ApiResponse<HealthCareVisitDTO> close(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody HealthCareRequests.CloseVisit p) {
        return ok("Visit closed", closeUseCase.close(school, id, p.toClosing(),
                Boolean.TRUE.equals(p.notifyParents()), userId));
    }

    @PostMapping("/visits/{id}/follow-up")
    @PreAuthorize(VIEW)
    public ApiResponse<HealthCareVisitDTO> followUp(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody HealthCareRequests.CloseVisit p) {
        return ok("Visit followed up", closeUseCase.followUp(school, id, p.toClosing(), p.reason(), userId));
    }

    @PostMapping("/visits/{id}/notes")
    @PreAuthorize(VIEW)
    public ApiResponse<HealthCareVisitDTO> addNote(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody HealthCareRequests.Text p) {
        return ok("Note added", noteUseCase.execute(school, id, p.text(), userId));
    }

    @PostMapping("/visits/{id}/void")
    @PreAuthorize(VIEW)
    public ApiResponse<HealthCareVisitDTO> voidVisit(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody HealthCareRequests.Reason p) {
        return ok("Visit voided", voidUseCase.execute(school, id, p.reason(), userId));
    }

    @PostMapping("/visits/{id}/notify-parents")
    @PreAuthorize(RECORD)
    public ApiResponse<HealthCareVisitDTO> notifyParents(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id) {
        var res = notifyUseCase.execute(school, id, userId);
        return ok(res.notifyProblem() == null ? "Parents notified"
                : "NONE".equals(res.notifyProblem()) ? "No parent could be notified in the app. Contact them directly."
                        : "Some parents could not be notified. Try again or contact them directly.", res);
    }

    // Settings

    @GetMapping("/settings")
    @PreAuthorize(VIEW)
    public ApiResponse<HealthCareSettingDTO> settings(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return ok("Health care settings fetched successfully", service.settings(school));
    }

    @PutMapping("/settings")
    @PreAuthorize(MANAGE_SETTINGS)
    public ApiResponse<HealthCareSettingDTO> updateSettings(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @RequestBody HealthCareRequests.Settings p) {
        return ok("Health care settings saved", settingUseCase.execute(school, p.toValues(), userId));
    }

    // Reports

    @GetMapping("/reports/daily")
    @PreAuthorize(VIEW)
    public ApiResponse<List<HealthCareVisitDTO>> dailyLog(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate date) {
        return ok("Daily log fetched successfully", service.dailyLog(school, date));
    }

    @GetMapping("/reports/summary")
    @PreAuthorize(VIEW)
    public ApiResponse<Summary> summary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ok("Summary fetched successfully", service.summary(school, from, to));
    }

    @GetMapping("/reports/frequent")
    @PreAuthorize(VIEW)
    public ApiResponse<List<FrequentVisitor>> frequent(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ok("Frequent visitors fetched successfully", service.frequentVisitors(school, from, to));
    }

    @GetMapping("/reports/limits")
    @PreAuthorize(VIEW)
    public ApiResponse<Limits> limits(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ok("Limits report fetched successfully", service.limits(school, from, to));
    }

    @GetMapping("/reports/periods")
    @PreAuthorize(VIEW)
    public ApiResponse<Periods> periods(@AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return ok("Report periods fetched successfully", service.periods(school));
    }

    @GetMapping("/reports/trends")
    @PreAuthorize(VIEW)
    public ApiResponse<Trends> trends(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String scope,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String yearId) {
        return ok("Trends fetched successfully", service.trends(school, trendScope(scope), month, termId, yearId));
    }

    @GetMapping("/students/{studentId}/history")
    @PreAuthorize(VIEW)
    public ApiResponse<StudentHistory> studentHistory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId) {
        return ok("Health care history fetched successfully", service.studentHistory(school, studentId));
    }

    // Exports

    @GetMapping("/export/daily")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportDaily(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate date, @RequestParam(defaultValue = "csv") String format) {
        return file(exporter.dailyLog(date, service.dailyLog(school, date), format));
    }

    @GetMapping("/export/summary")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportSummary(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from, @RequestParam LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {
        return file(exporter.summary(service.summary(school, from, to), format));
    }

    @GetMapping("/export/frequent")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportFrequent(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from, @RequestParam LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {
        return file(exporter.frequent(from, to, service.frequentVisitors(school, from, to), format));
    }

    @GetMapping("/export/limits")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportLimits(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from, @RequestParam LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {
        return file(exporter.limits(service.limits(school, from, to), format));
    }

    @GetMapping("/export/trends")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportTrends(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String scope,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String termId,
            @RequestParam(required = false) String yearId,
            @RequestParam(defaultValue = "csv") String format) {
        return file(exporter.trends(service.trends(school, trendScope(scope), month, termId, yearId), format));
    }

    @GetMapping("/export/students/{studentId}")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportStudent(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId, @RequestParam(defaultValue = "csv") String format) {
        return file(exporter.student(service.studentHistory(school, studentId), format));
    }

    private static Filter filter(String status) {
        try {
            return Filter.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status must be ALL, OPEN, FOLLOW_UP, CLOSED or VOIDED");
        }
    }

    private static HealthCareService.TrendScope trendScope(String scope) {
        try {
            return HealthCareService.TrendScope.valueOf(scope.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Scope must be MONTH, TERM or YEAR");
        }
    }

    private static Outcome outcome(String outcome) {
        if (outcome == null || outcome.isBlank()) {
            return null;
        }
        try {
            return Outcome.valueOf(outcome.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Outcome must be RETURNED_TO_CLASS, SENT_HOME or REFERRED");
        }
    }

    private static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("success", 200, message, data);
    }

    private static <T> ApiResponse<List<T>> paged(Page<T> res, String message) {
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());
        return new ApiResponse<>("success", 200, message, res.getContent(), meta);
    }

    private static ResponseEntity<byte[]> file(ReportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                .body(file.data());
    }
}
