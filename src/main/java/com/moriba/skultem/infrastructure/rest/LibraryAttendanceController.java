package com.moriba.skultem.infrastructure.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.LibraryAttendanceOverviewDTO;
import com.moriba.skultem.application.dto.LibraryAttendanceTotalsDTO;
import com.moriba.skultem.application.dto.LibraryCheckInResultDTO;
import com.moriba.skultem.application.dto.LibrarySessionDTO;
import com.moriba.skultem.application.dto.LibrarySessionDetailDTO;
import com.moriba.skultem.application.dto.LibrarySettingDTO;
import com.moriba.skultem.application.dto.LibraryStudentHistoryDTO;
import com.moriba.skultem.application.dto.LibraryVisitDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.services.LibraryAttendanceService;
import com.moriba.skultem.application.services.LibraryAttendanceService.SessionFilter;
import com.moriba.skultem.application.usecase.MarkLibrarySessionUseCase.Mark;
import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.repository.LibraryVisitRepository.Filter;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.idempotency.Idempotent;
import com.moriba.skultem.infrastructure.report.LibraryAttendanceExporter;
import com.moriba.skultem.infrastructure.report.ReportFile;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.LibraryAttendanceRequests;
import com.moriba.skultem.infrastructure.security.RequiresModule;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Library Attendance (docs/LIBRARY_ATTENDANCE.md): independent library visits and scheduled class library
 * sessions. Part of the Library module.
 * <p>
 * Who may do what (§4): the librarian records; principal-level roles supervise (follow up visits, cancel,
 * export) and are the only ones who correct a finalized register; a plain ADMIN can look. Teachers get nothing
 * here - unlike the book catalogue, whose VIEW gate lets them in. SUPER_ADMIN passes any gate naming LIBRARIAN
 * (PermissionService.SUPER_ADMIN_COVERS). Not section-scoped: a section-limited admin is refused, as on the rest
 * of the library.
 */
@RequiresModule(FeatureModule.LIBRARY)
@RestController
@RequestMapping("/api/v1/library/attendance")
@RequiredArgsConstructor
public class LibraryAttendanceController {

    private static final String RECORD = "@permissionService.hasAnySchoolRole(#school, 'LIBRARIAN')";
    private static final String VIEW = "@permissionService.hasAnySchoolRole(#school, 'LIBRARIAN', 'PRINCIPAL', 'OWNER', 'PROPRIETOR', 'SUPER_ADMIN', 'ADMIN')";
    private static final String SUPERVISE = "@permissionService.hasAnySchoolRole(#school, 'LIBRARIAN', 'PRINCIPAL', 'OWNER', 'PROPRIETOR', 'SUPER_ADMIN')";
    private static final String CORRECT = "@permissionService.hasAnySchoolRole(#school, 'PRINCIPAL', 'OWNER', 'PROPRIETOR', 'SUPER_ADMIN')";
    // Choosing the Library subject is set-up, like the rest of Library Settings.
    private static final String CONFIGURE = "@permissionService.hasAnySchoolRole(#school, 'LIBRARIAN', 'PRINCIPAL', 'OWNER', 'PROPRIETOR', 'SUPER_ADMIN', 'ADMIN')";

    private final LibraryAttendanceService service;
    private final LibraryAttendanceExporter exporter;

    @GetMapping("/overview")
    @PreAuthorize(VIEW)
    public ApiResponse<LibraryAttendanceOverviewDTO> overview(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return ok("Library attendance overview fetched successfully", service.overview(school));
    }

    @GetMapping("/subjects")
    @PreAuthorize(CONFIGURE)
    public ApiResponse<List<LibraryAttendanceService.SubjectOption>> subjects(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school) {
        return ok("Subjects fetched successfully", service.subjects(school));
    }

    @PutMapping("/subject")
    @PreAuthorize(CONFIGURE)
    public ApiResponse<LibrarySettingDTO> setLibrarySubject(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestBody LibraryAttendanceRequests.SetLibrarySubject param) {
        return ok("Library subject saved successfully", service.setLibrarySubject(school, param.subjectId()));
    }

    // Independent visits

    @GetMapping("/visits")
    @PreAuthorize(VIEW)
    public ApiResponse<List<LibraryVisitDTO>> listVisits(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String status) {
        return paged(service.listVisits(school, page, size, from, to, studentId, search, visitFilter(status)),
                "Library visits fetched successfully");
    }

    @PostMapping("/visits/check-in")
    @PreAuthorize(RECORD)
    @Idempotent(operation = "library.visit.check-in")
    public ApiResponse<LibraryCheckInResultDTO> checkIn(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody LibraryAttendanceRequests.CheckIn param) {
        var res = service.checkIn(school, param.studentIds(), Boolean.TRUE.equals(param.eligibleOnly()), userId);
        String message = res.checkedIn().isEmpty() ? "No students were checked in"
                : res.checkedIn().size() == 1 ? "Student checked in" : res.checkedIn().size() + " students checked in";
        return ok(message, res);
    }

    @PostMapping("/visits/{id}/check-out")
    @PreAuthorize(RECORD)
    public ApiResponse<LibraryVisitDTO> checkOut(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id) {
        return ok("Student checked out", service.checkOut(school, id, userId));
    }

    @PostMapping("/visits/{id}/resolve")
    @PreAuthorize(SUPERVISE)
    public ApiResponse<LibraryVisitDTO> resolve(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody LibraryAttendanceRequests.Reason param) {
        return ok("Visit followed up", service.resolve(school, id, param.reason(), userId));
    }

    // Scheduled sessions

    @GetMapping("/sessions")
    @PreAuthorize(VIEW)
    public ApiResponse<List<LibrarySessionDTO>> listSessions(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) String classSessionId,
            @RequestParam(defaultValue = "ALL") String status) {
        return ok("Library sessions fetched successfully",
                service.listSessions(school, from, to, classSessionId, sessionFilter(status)));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize(VIEW)
    public ApiResponse<LibrarySessionDetailDTO> session(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        return ok("Library session fetched successfully", service.session(school, id));
    }

    @PostMapping("/sessions/open")
    @PreAuthorize(RECORD)
    public ApiResponse<LibrarySessionDetailDTO> open(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody LibraryAttendanceRequests.SessionSlot param) {
        return ok("Library session opened", service.open(school, param.timetableEntryId(), param.date(), userId));
    }

    @PostMapping("/sessions/cancel")
    @PreAuthorize(SUPERVISE)
    public ApiResponse<LibrarySessionDetailDTO> cancelUpcoming(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody LibraryAttendanceRequests.CancelSlot param) {
        return ok("Library session cancelled",
                service.cancelUpcoming(school, param.timetableEntryId(), param.date(), param.reason(), userId));
    }

    @PostMapping("/sessions/{id}/cancel")
    @PreAuthorize(SUPERVISE)
    public ApiResponse<LibrarySessionDetailDTO> cancel(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody LibraryAttendanceRequests.Reason param) {
        return ok("Library session cancelled", service.cancel(school, id, param.reason(), userId));
    }

    @PutMapping("/sessions/{id}/marks")
    @PreAuthorize(RECORD)
    public ApiResponse<LibrarySessionDetailDTO> mark(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody LibraryAttendanceRequests.Marks param) {
        var marks = param.marks().stream()
                .map(m -> new Mark(m.recordId(), Status.valueOf(m.status()), m.excuseReason()))
                .toList();
        return ok("Register saved", service.mark(school, id, marks, userId));
    }

    @PostMapping("/sessions/{id}/roster")
    @PreAuthorize(RECORD)
    public ApiResponse<LibrarySessionDetailDTO> addToRoster(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @Valid @RequestBody LibraryAttendanceRequests.AddToRoster param) {
        return ok("Student added to the register",
                service.addToRoster(school, id, param.studentId(), param.reason(), userId));
    }

    @PostMapping("/sessions/{id}/roster/{recordId}/remove")
    @PreAuthorize(RECORD)
    public ApiResponse<LibrarySessionDetailDTO> removeFromRoster(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @PathVariable String recordId,
            @Valid @RequestBody LibraryAttendanceRequests.Reason param) {
        return ok("Student removed from the register",
                service.removeFromRoster(school, id, recordId, param.reason(), userId));
    }

    @PostMapping("/sessions/{id}/finalize")
    @PreAuthorize(RECORD)
    public ApiResponse<LibrarySessionDetailDTO> finalizeSession(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id) {
        return ok("Register finalized", service.finalizeSession(school, id, userId));
    }

    @PostMapping("/sessions/{id}/records/{recordId}/correct")
    @PreAuthorize(CORRECT)
    public ApiResponse<LibrarySessionDetailDTO> correct(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String id,
            @PathVariable String recordId,
            @Valid @RequestBody LibraryAttendanceRequests.Correct param) {
        return ok("Attendance corrected", service.correct(school, id, recordId, Status.valueOf(param.status()),
                param.excuseReason(), param.reason(), userId));
    }

    // Reports

    @GetMapping("/students/{studentId}/history")
    @PreAuthorize(VIEW)
    public ApiResponse<LibraryStudentHistoryDTO> studentHistory(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId) {
        return ok("Library history fetched successfully", service.studentHistory(school, studentId));
    }

    @GetMapping("/totals")
    @PreAuthorize(VIEW)
    public ApiResponse<LibraryAttendanceTotalsDTO> totals(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) String classSessionId) {
        return ok("Library attendance totals fetched successfully", service.totals(school, from, to, classSessionId));
    }

    @GetMapping("/export/visits")
    @PreAuthorize(SUPERVISE)
    public ResponseEntity<byte[]> exportVisits(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status) {
        var visits = service.listVisits(school, 1, 0, from, to, null, null, visitFilter(status)).getContent();
        return file(exporter.visits(visits, format));
    }

    @GetMapping("/export/sessions")
    @PreAuthorize(SUPERVISE)
    public ResponseEntity<byte[]> exportSessions(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) String classSessionId,
            @RequestParam(defaultValue = "ALL") String status) {
        return file(exporter.sessions(
                service.listSessions(school, from, to, classSessionId, sessionFilter(status)), format));
    }

    @GetMapping("/export/totals")
    @PreAuthorize(SUPERVISE)
    public ResponseEntity<byte[]> exportTotals(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) String classSessionId) {
        return file(exporter.totals(service.totals(school, from, to, classSessionId), format));
    }

    @GetMapping("/export/students/{studentId}")
    @PreAuthorize(SUPERVISE)
    public ResponseEntity<byte[]> exportStudent(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String studentId,
            @RequestParam(defaultValue = "csv") String format) {
        return file(exporter.student(service.studentHistory(school, studentId), format));
    }

    private static Filter visitFilter(String status) {
        try {
            return Filter.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status must be ALL, OPEN, UNRESOLVED, CHECKED_OUT or RESOLVED");
        }
    }

    private static SessionFilter sessionFilter(String status) {
        try {
            return SessionFilter.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Status must be ALL, UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED or INCOMPLETE");
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
