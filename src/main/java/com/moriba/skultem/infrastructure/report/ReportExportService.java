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

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.ReportResponse;
import com.moriba.skultem.application.dto.StudentAssessmentDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.usecase.AttendanceReportUseCase;
import com.moriba.skultem.application.usecase.ClassReportUseCase;
import com.moriba.skultem.application.dto.FeePaymentRowDTO;
import com.moriba.skultem.application.dto.StudentFeeBalanceDTO;
import com.moriba.skultem.application.usecase.ExpenseReportUseCase;
import com.moriba.skultem.application.usecase.FeeReportUseCase;
import com.moriba.skultem.application.usecase.GetPaymentHistoryReportUseCase;
import com.moriba.skultem.application.usecase.GetStudentFeeBalancesUseCase;
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
        private final GetStudentFeeBalancesUseCase getStudentFeeBalancesUseCase;
        private final GetPaymentHistoryReportUseCase getPaymentHistoryReportUseCase;

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

        // The Student Balances report's rows, unpaginated - school fees only (platform fee excluded,
        // see GetStudentFeeBalancesUseCase). Same filters the on-screen report supports.
        public ReportFile exportStudentFeeBalances(String schoolId, String format, String academicYearId,
                        String termId, String classSessionId, String status, String feeCategoryId) {
                Page<StudentFeeBalanceDTO> page = getStudentFeeBalancesUseCase.execute(schoolId, academicYearId,
                                termId, classSessionId, status, feeCategoryId, null, null,
                                GetStudentFeeBalancesUseCase.SortBy.BALANCE, false, 0, 0);
                List<StudentFeeBalanceDTO> records = page.getContent();

                List<String> headers = List.of("Student", "Admission No", "Class", "Expected", "Paid", "Balance",
                                "Status");
                List<List<String>> rows = records.stream()
                                .map(r -> List.of(
                                                safe(r.studentName()),
                                                safe(r.admissionNumber()),
                                                safe(r.className()),
                                                safe(r.expected()),
                                                safe(r.paid()),
                                                safe(r.balance()),
                                                safe(r.status())))
                                .toList();

                return build("student-fee-balances", "Student Fee Balances Report", headers, rows, format);
        }

        // The Payment History report's transactions, unpaginated - school fees only (platform fee
        // excluded, see GetPaymentHistoryReportUseCase).
        public ReportFile exportSchoolPaymentHistory(String schoolId, String format, LocalDate startDate,
                        LocalDate endDate, String academicYearId, String termId, String classSessionId,
                        String studentId, String method) {
                var zone = ZoneId.systemDefault();
                Instant from = startDate != null ? startDate.atStartOfDay(zone).toInstant() : null;
                Instant to = endDate != null ? endDate.plusDays(1).atStartOfDay(zone).toInstant() : null;
                var parsedMethod = (method != null && !method.isBlank())
                                ? com.moriba.skultem.domain.model.Payment.PaymentMethod.valueOf(method)
                                : null;

                Page<FeePaymentRowDTO> page = getPaymentHistoryReportUseCase.execute(schoolId, from, to,
                                academicYearId, termId, classSessionId, studentId, parsedMethod, null, 0, 0);
                List<FeePaymentRowDTO> records = page.getContent();

                List<String> headers = List.of("Date", "Receipt", "Student", "Class", "Fee Type", "Amount", "Method",
                                "Recorded By");
                List<List<String>> rows = records.stream()
                                .map(r -> List.of(
                                                formatInstant(r.paidAt()),
                                                safe(r.receiptNo()),
                                                safe(r.studentName()),
                                                safe(r.className()),
                                                safe(r.feeCategoryName()),
                                                safe(r.amount()),
                                                safe(r.method()),
                                                safe(r.recordedBy())))
                                .toList();

                return build("fee-payment-history", "Payment History Report", headers, rows, format);
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

        // The fee structures of one academic year - the one asked for, or the school's active year - the same
        // scope as the Fee Structures page this exports.
        public ReportFile exportFees(String schoolId, String format, String classId, LocalDate startDate,
                        LocalDate endDate, String academicYearId) {
                var page = listFeeStructureBySchoolUseCase.execute(schoolId, academicYearId, 0, 0, null, null, null,
                                null, null, null, null);
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

        // Hard cap on rows returned by the generic builder-driven export below. Unlike the
        // entity-specific export endpoints (exportPayments/exportFees/etc. above) which are
        // already narrowed by required params like classId/date range, this export accepts
        // arbitrary report-builder filters that could otherwise match a school's entire history
        // (e.g. "payments" with no filters at all). Capping here avoids an accidentally
        // unbounded query locking up the server/DB for a large school.
        private static final int EXPORT_ROW_CAP = 5000;

        // Generic export for the Report Builder (backs POST /report/export/run/download):
        // mirrors runReport()'s entity+filter resolution, but instead of returning a JSON page
        // it pulls up to EXPORT_ROW_CAP rows via the same use case runReport() would call and
        // renders them to CSV/PDF using the same column set shown in the on-screen builder
        // tables (see app/components/report/table/*.vue).
        public ReportFile exportBuilderReport(String schoolId, RunReportDTO param, String format,
                        String academicYearId) {
                String type = normalizeType(param.entity());

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
                        case "students" -> exportStudentsBuilder(report, format);
                        case "teachers" -> exportTeachersBuilder(report, format);
                        case "classes" -> exportClassesBuilder(report, format);
                        case "subjects" -> exportSubjectsBuilder(report, format);
                        case "attendances" -> exportAttendancesBuilder(report, format);
                        case "fees" -> exportStudentFeesBuilder(report, format);
                        case "payments" -> exportPaymentsBuilder(report, format);
                        case "grades" -> exportGradesBuilder(report, format);
                        default -> throw new NotFoundException("Unsupported report type for export");
                };
        }

        private ReportFile exportStudentsBuilder(ReportBuilderDTO report, String format) {
                List<StudentDTO> records = studentReportUseCase.execute(report, 0, EXPORT_ROW_CAP).getContent();

                List<String> headers = List.of("Name", "Age", "Gender", "Class", "Guardian", "Guardian Phone",
                                "City", "Street", "Status");
                List<List<String>> rows = records.stream()
                                .map(s -> List.of(
                                                (safe(s.givenNames()) + " " + safe(s.familyName())).trim(),
                                                s.age() == null ? "" : s.age() + " Years",
                                                s.gender() == null ? "" : s.gender().name(),
                                                safe(s.className()),
                                                s.guardian() == null ? ""
                                                                : (safe(s.guardian().givenNames()) + " "
                                                                                + safe(s.guardian().familyName()))
                                                                                .trim(),
                                                s.guardian() == null ? "" : safe(s.guardian().phone()),
                                                safe(s.city()),
                                                safe(s.street()),
                                                s.status() == null ? "" : s.status().name()))
                                .toList();

                return build("students", "Students Report", headers, rows, format);
        }

        private ReportFile exportTeachersBuilder(ReportBuilderDTO report, String format) {
                List<TeacherDTO> records = teacherReportUseCase.execute(report, 0, EXPORT_ROW_CAP).getContent();

                List<String> headers = List.of("Name", "Gender", "Email", "Phone", "City", "Street", "Status");
                List<List<String>> rows = records.stream()
                                .map(t -> List.of(
                                                ((t.title() == null ? "" : t.title().name() + " ")
                                                                + safe(t.user() == null ? null : t.user().givenNames())
                                                                + " "
                                                                + safe(t.user() == null ? null : t.user().familyName()))
                                                                .trim(),
                                                t.gender() == null ? "" : t.gender().name(),
                                                t.user() == null ? "" : safe(t.user().email()),
                                                safe(t.phone()),
                                                safe(t.city()),
                                                safe(t.street()),
                                                safe(t.status())))
                                .toList();

                return build("teachers", "Teachers Report", headers, rows, format);
        }

        private ReportFile exportClassesBuilder(ReportBuilderDTO report, String format) {
                List<ClassSessionDTO> records = classReportUseCase.execute(report, 0, EXPORT_ROW_CAP).getContent();

                List<String> headers = List.of("Name", "Grade", "Level", "Section", "Stream", "Students",
                                "Class Teacher");
                List<List<String>> rows = records.stream()
                                .map(c -> List.of(
                                                safe(c.clazz()),
                                                safe(c.grade()),
                                                safe(c.classLevel()),
                                                safe(c.sectionName()),
                                                safe(c.streamName()),
                                                String.valueOf(c.totalStudent()),
                                                safe(c.teacherName())))
                                .toList();

                return build("classes", "Classes Report", headers, rows, format);
        }

        private ReportFile exportSubjectsBuilder(ReportBuilderDTO report, String format) {
                List<TeacherSubjectDTO> records = subjectReportUseCase.execute(report, 0, EXPORT_ROW_CAP)
                                .getContent();

                List<String> headers = List.of("Subject", "Class", "Section", "Stream", "Teacher");
                List<List<String>> rows = records.stream()
                                .map(s -> List.of(
                                                safe(s.subjectName()),
                                                safe(s.className()),
                                                safe(s.sectionName()),
                                                safe(s.streamName()),
                                                safe(s.teacherName())))
                                .toList();

                return build("subjects", "Subjects Report", headers, rows, format);
        }

        private ReportFile exportAttendancesBuilder(ReportBuilderDTO report, String format) {
                List<AttendanceDTO> records = attendanceReportUseCase.execute(report, 0, EXPORT_ROW_CAP)
                                .getContent();

                List<String> headers = List.of("Student", "Class", "State", "Date", "Reason");
                List<List<String>> rows = records.stream()
                                .map(a -> List.of(
                                                safe(a.student()),
                                                safe(a.clazz()),
                                                safe(a.state()),
                                                formatDate(a.date()),
                                                safe(a.reason())))
                                .toList();

                return build("attendances", "Attendance Report", headers, rows, format);
        }

        private ReportFile exportStudentFeesBuilder(ReportBuilderDTO report, String format) {
                List<StudentFeeDTO> records = feeReportUseCase.execute(report, 0, EXPORT_ROW_CAP).getContent();

                List<String> headers = List.of("Student", "Class", "Fee", "Term", "Amount", "Amount Paid",
                                "Outstanding", "Status");
                List<List<String>> rows = records.stream()
                                .map(f -> List.of(
                                                safe(f.student()),
                                                safe(f.clazz()),
                                                safe(f.fee()),
                                                safe(f.term()),
                                                safe(f.amount()),
                                                safe(f.amountPaid()),
                                                safe(f.outstanding()),
                                                safe(f.status())))
                                .toList();

                return build("fees", "Fees Report", headers, rows, format);
        }

        private ReportFile exportPaymentsBuilder(ReportBuilderDTO report, String format) {
                List<PaymentDTO> records = paymentReportUseCase.execute(report, 0, EXPORT_ROW_CAP).getContent();

                List<String> headers = List.of("Student", "Fee", "Amount", "Paid On", "Payment Method",
                                "Reference No");
                List<List<String>> rows = records.stream()
                                .map(p -> List.of(
                                                safe(p.student()),
                                                safe(p.fee()),
                                                safe(p.amount()),
                                                formatInstant(p.paidAt()),
                                                p.paymentMethod() == null ? "" : p.paymentMethod().name(),
                                                safe(p.referenceNo())))
                                .toList();

                return build("payments", "Payments Report", headers, rows, format);
        }

        private ReportFile exportGradesBuilder(ReportBuilderDTO report, String format) {
                List<AssessmentScoreDTO> records = gradeReportUseCase.execute(report, 0, EXPORT_ROW_CAP)
                                .getContent();

                List<String> headers = List.of("Student", "Subject", "Assessment", "Term", "Class", "Teacher",
                                "State", "Score", "Weight", "Weight Score");
                List<List<String>> rows = records.stream()
                                .map(g -> List.of(
                                                safe(g.student()),
                                                safe(g.subject()),
                                                safe(g.name()),
                                                safe(g.term()),
                                                safe(g.clazz()),
                                                safe(g.teacher()),
                                                safe(g.status()),
                                                safe(g.score()),
                                                g.weight() == null ? "" : g.weight() + "%",
                                                g.weightScore() == null ? "" : g.weightScore() + "%"))
                                .toList();

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
