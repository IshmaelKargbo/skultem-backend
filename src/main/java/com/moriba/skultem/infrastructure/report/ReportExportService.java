package com.moriba.skultem.infrastructure.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.ReportResponse;
import com.moriba.skultem.application.dto.StudentAssessmentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.usecase.AttendanceReportUseCase;
import com.moriba.skultem.application.usecase.ClassReportUseCase;
import com.moriba.skultem.application.usecase.ExpenseReportUseCase;
import com.moriba.skultem.application.usecase.FeeReportUseCase;
import com.moriba.skultem.application.usecase.GradeReportUseCase;
import com.moriba.skultem.application.usecase.LeaderBoardReportUseCase;
import com.moriba.skultem.application.usecase.ListBehaviourBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListFeeStructureBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListStudentAssessmentTermUseCase;
import com.moriba.skultem.application.usecase.PaymentReportUseCase;
import com.moriba.skultem.application.usecase.ScopeReportToAcademicYearUseCase;
import com.moriba.skultem.application.usecase.SimplifiedClassLeaderBoardUseCase;
import com.moriba.skultem.application.usecase.StudentLedgerEntryReportUseCase;
import com.moriba.skultem.application.usecase.StudentReportUseCase;
import com.moriba.skultem.application.usecase.SubjectReportUseCase;
import com.moriba.skultem.application.usecase.TeacherReportUseCase;
import com.moriba.skultem.application.usecase.TransactionReportUseCase;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;
import com.moriba.skultem.infrastructure.rest.dto.RunReportDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportExportService {

        private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
        private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final AttendanceReportUseCase attendanceReportUseCase;
        private final ListBehaviourBySchoolUseCase listBehaviourBySchoolUseCase;
        private final StudentReportUseCase studentReportUseCase;
        private final TeacherReportUseCase teacherReportUseCase;
        private final PaymentReportUseCase paymentReportUseCase;
        private final TransactionReportUseCase transactionReportUseCase;
        private final ExpenseReportUseCase expenseReportUseCase;
        private final FeeReportUseCase feeReportUseCase;
        private final ClassReportUseCase classReportUseCase;
        private final GradeReportUseCase gradeReportUseCase;
        private final LeaderBoardReportUseCase leaderBoardReportUseCase;
        private final SimplifiedClassLeaderBoardUseCase simplifiedClassLeaderBoardUseCase;
        private final SubjectReportUseCase subjectReportUseCase;
        private final ListFeeStructureBySchoolUseCase listFeeStructureBySchoolUseCase;
        private final ListStudentAssessmentTermUseCase listStudentAssessmentTermUseCase;
        private final ScopeReportToAcademicYearUseCase scopeReportToAcademicYearUseCase;
        private final StudentLedgerEntryReportUseCase studentLedgerEntryReportUseCase;

        public ReportFile exportPayments(String schoolId, String format, String classId, LocalDate startDate,
                        LocalDate endDate) {
                var dto = new ReportBuilderDTO(schoolId, "payments", dateRangeFilter("paidAt", startDate, endDate));
                var page = paymentReportUseCase.execute(dto, 0, 0);
                List<PaymentDTO> records = page.getContent();

                List<String> headers = List.of("Paid At", "Student", "Fee Category", "Term", "Amount", "Method",
                                "Reference");
                List<List<String>> rows = records.stream()
                                .map(r -> List.of(
                                                formatInstant(r.paidAt()),
                                                safe(r.student()),
                                                safe(r.fee()),
                                                safe(r.term()),
                                                safe(r.amount()),
                                                r.paymentMethod() == null ? "" : r.paymentMethod().name(),
                                                safe(r.referenceNo())))
                                .toList();

                return build("payments", "Payments Report", headers, rows, format);
        }

        // BETWEEN/GREATER_THAN/LESS_THAN on the given field, whichever of startDate/endDate were
        // actually supplied - mirrors the date-range filter shape the /report/export/run and
        // /widget/run endpoints already accept, so the same "from/to" pair works here too.
        private List<Filter> dateRangeFilter(String field, LocalDate startDate, LocalDate endDate) {
                if (startDate == null && endDate == null) {
                        return List.of();
                }

                if (startDate != null && endDate != null) {
                        return List.of(new Filter(field, FilterOperator.BETWEEN, "instant", startDate.toString(),
                                        endDate.toString(), null));
                }

                return startDate != null
                                ? List.of(new Filter(field, FilterOperator.GREATER_THAN, "instant",
                                                startDate.toString(), null, null))
                                : List.of(new Filter(field, FilterOperator.LESS_THAN, "instant", endDate.toString(),
                                                null, null));
        }

        public ReportFile exportAttendance(String schoolId, String classSessionId, String format, LocalDate startDate,
                        LocalDate endDate) {
                var page = attendanceReportUseCase.execute(schoolId, classSessionId, null, 0, 0);
                List<AttendanceHistoryDTO> records = page.getContent();

                List<String> headers = List.of("Date", "Class", "Present", "Total", "Rate");
                List<List<String>> rows = records.stream()
                                .filter(r -> inRange(r.date(), startDate, endDate))
                                .map(r -> {
                                        long total = Optional.ofNullable(r.totalCount()).orElse(0L);
                                        long present = Optional.ofNullable(r.presentCount()).orElse(0L);
                                        String rate = total > 0 ? String.format("%.2f%%", (present * 100.0) / total)
                                                        : "0%";
                                        return List.of(
                                                        formatDate(r.date()),
                                                        safe(r.className()),
                                                        String.valueOf(present),
                                                        String.valueOf(total),
                                                        rate);
                                })
                                .toList();

                return build("attendance", "Attendance Report", headers, rows, format);
        }

        public ReportFile exportBehaviour(String schoolId, String classId, String format, LocalDate startDate,
                        LocalDate endDate) {
                var page = listBehaviourBySchoolUseCase.execute(schoolId, classId, null, 0, 0);
                List<BehaviourDTO> records = page.getContent();

                List<String> headers = List.of("Student", "Kind", "Category", "Note", "Created At");
                List<List<String>> rows = records.stream()
                                .filter(r -> inRange(toLocalDate(r.createdAt()), startDate, endDate))
                                .map(r -> List.of(
                                                safe(r.student()),
                                                safe(r.kind()),
                                                safe(r.category()),
                                                safe(r.note()),
                                                formatInstant(r.createdAt())))
                                .toList();

                return build("behaviour", "Behaviour Report", headers, rows, format);
        }

        public ReportFile exportFees(String schoolId, String format, String classId, LocalDate startDate,
                        LocalDate endDate) {
                var page = listFeeStructureBySchoolUseCase.execute(schoolId, 0, 0, null, null, null, null, null, null,
                                null);
                List<FeeStructureDTO> records = page.getContent();

                List<String> headers = List.of("Class", "Term", "Category", "Amount", "Due Date", "Installment",
                                "Academic Year");
                List<List<String>> rows = records.stream()
                                .filter(r -> classId == null || (r.clazz() != null && classId.equals(r.clazz().id())))
                                .filter(r -> inRange(r.dueDate(), startDate, endDate))
                                .map(r -> List.of(
                                                r.clazz() != null ? r.clazz().name() : "All Classes",
                                                safe(r.term().name()),
                                                safe(r.category().name()),
                                                safe(r.amount()),
                                                formatDate(r.dueDate()),
                                                String.valueOf(r.allowInstallment()),
                                                safe(r.academicYear().name())))
                                .toList();

                return build("fees", "Fee Structure Report", headers, rows, format);
        }

        public ReportFile exportGrades(String schoolId, String teacherSubjectId, String termId, String format) {
                List<StudentAssessmentDTO> records = listStudentAssessmentTermUseCase.execute(schoolId,
                                teacherSubjectId,
                                termId);

                List<String> headers = List.of("Student", "Assessment", "Score", "Grade", "Weight", "Weight Score",
                                "Status");
                List<List<String>> rows = new ArrayList<>();

                for (StudentAssessmentDTO assessment : records) {
                        assessment.scores().forEach(score -> rows.add(List.of(
                                        safe(assessment.name()),
                                        safe(score.assessment()),
                                        safe(score.score()),
                                        safe(score.grade()),
                                        safe(score.weight()),
                                        safe(score.weightScore()),
                                        safe(score.status()))));
                }

                return build("grades", "Grades Report", headers, rows, format);
        }

        public ReportResponse<?> runReport(String schoolId, RunReportDTO param, int page, int size) {
                return runReport(schoolId, param, page, size, null);
        }

        public ReportResponse<?> runReport(String schoolId, RunReportDTO param, int page, int size,
                        String academicYearId) {

                String type = normalizeType(param.entity());
                int limit = size > 0 ? Math.min(size, 200) : 50;

                List<Filter> filters = param.filters()
                                .stream()
                                .map(e -> new Filter(
                                                e.field(),
                                                e.operator(),
                                                e.type(),
                                                e.value(),
                                                e.valueTo(),
                                                e.values()))
                                .toList();

                filters = scopeReportToAcademicYearUseCase.execute(schoolId, type, filters, academicYearId);

                var report = new ReportBuilderDTO(schoolId, param.entity(), filters);

                return switch (type) {
                        case "students" -> buildResponse(studentReportUseCase.execute(report, page, limit));
                        case "teachers" -> buildResponse(teacherReportUseCase.execute(report, page, limit));
                        case "classes" -> buildResponse(classReportUseCase.execute(report, page, limit));
                        case "subjects" -> buildResponse(subjectReportUseCase.execute(report, page, limit));
                        case "attendances" -> buildResponse(attendanceReportUseCase.execute(report, page, limit));
                        case "fees" -> buildResponse(feeReportUseCase.execute(report, page, limit));
                        case "payments" -> buildResponse(paymentReportUseCase.execute(report, page, limit));
                        case "expenses" -> buildResponse(expenseReportUseCase.execute(report, page, limit));
                        case "leaderboard" -> buildResponse(leaderBoardReportUseCase.execute(report, page, limit));
                        case "transactions" -> buildResponse(transactionReportUseCase.execute(report, page, limit));
                        case "ledger" -> buildResponse(studentLedgerEntryReportUseCase.execute(report, page, limit));
                        case "breakdown" ->
                                buildResponse(simplifiedClassLeaderBoardUseCase.execute(report, page, limit));
                        case "grades" -> buildResponse(gradeReportUseCase.execute(report, page, limit));
                        default -> throw new NotFoundException("Unsupported report type");
                };
        }

        private String normalizeType(String type) {
                if (type == null) {
                        return "";
                }
                return type.trim().toLowerCase();
        }

        private ReportFile build(String baseName, String title, List<String> headers, List<List<String>> rows,
                        String format) {
                String normalized = normalizeFormat(format);
                byte[] data = "pdf".equals(normalized)
                                ? ReportExportUtil.toPdf(title, headers, rows)
                                : ReportExportUtil.toCsv(headers, rows);
                String ext = "pdf".equals(normalized) ? "pdf" : "csv";
                String filename = baseName + "-" + LocalDate.now() + "." + ext;
                String contentType = "pdf".equals(normalized) ? "application/pdf" : "text/csv";
                return new ReportFile(filename, contentType, data);
        }

        private String normalizeFormat(String format) {
                if (format == null) {
                        return "csv";
                }
                String value = format.trim().toLowerCase();
                return value.equals("pdf") ? "pdf" : "csv";
        }

        private String formatDate(LocalDate date) {
                return date == null ? "" : DATE.format(date);
        }

        private String formatInstant(Instant instant) {
                if (instant == null) {
                        return "";
                }
                return DATETIME.format(instant.atZone(ZoneId.systemDefault()));
        }

        private boolean inRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
                if (date == null) {
                        return false;
                }
                if (startDate != null && date.isBefore(startDate)) {
                        return false;
                }
                if (endDate != null && date.isAfter(endDate)) {
                        return false;
                }
                return true;
        }

        private LocalDate toLocalDate(Instant instant) {
                if (instant == null) {
                        return null;
                }
                return instant.atZone(ZoneId.systemDefault()).toLocalDate();
        }

        private String safe(Object value) {
                return value == null ? "" : value.toString();
        }

        private Map<String, Object> buildMeta(Page<?> res) {
                return Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages(),
                                "hasNext", res.hasNext(),
                                "hasPrevious", res.hasPrevious());
        }

        private <T> ReportResponse<List<T>> buildResponse(Page<T> page) {
                return new ReportResponse<>(page.getContent(), buildMeta(page));
        }
}
