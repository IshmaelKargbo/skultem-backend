package com.moriba.skultem.infrastructure.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.StudentAssessmentDTO;
import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.application.dto.ReportTableDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.usecase.AttendanceReportUseCase;
import com.moriba.skultem.application.usecase.ClassReportUseCase;
import com.moriba.skultem.application.usecase.GradeReportUseCase;
import com.moriba.skultem.application.usecase.ListBehaviourBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListFeeStructureBySchoolUseCase;
import com.moriba.skultem.application.usecase.ListStudentAssessmentTermUseCase;
import com.moriba.skultem.application.usecase.ListStudentPaymentBySchoolUseCase;
import com.moriba.skultem.application.usecase.StudentReportUseCase;
import com.moriba.skultem.application.usecase.SubjectReportUseCase;
import com.moriba.skultem.application.usecase.TeacherReportUseCase;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.infrastructure.rest.dto.RunReportDTO;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.domain.vo.Filter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportExportService {

        private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
        private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final StudentLedgerEntryRepository ledgerRepo;
        private final EnrollmentRepository enrollmentRepo;
        private final AcademicYearRepository academicYearRepo;
        private final ListStudentPaymentBySchoolUseCase listStudentPaymentBySchoolUseCase;
        private final AttendanceReportUseCase attendanceReportUseCase;
        private final ListBehaviourBySchoolUseCase listBehaviourBySchoolUseCase;
        private final StudentReportUseCase studentReportUseCase;
        private final TeacherReportUseCase teacherReportUseCase;
        private final ClassReportUseCase classReportUseCase;
        private final GradeReportUseCase gradeReportUseCase;
        private final SubjectReportUseCase subjectReportUseCase;
        private final ListFeeStructureBySchoolUseCase listFeeStructureBySchoolUseCase;
        private final ListStudentAssessmentTermUseCase listStudentAssessmentTermUseCase;

        public ReportFile exportLedger(String schoolId, String format, String classId, LocalDate startDate,
                        LocalDate endDate) {
                var table = buildLedgerTable(schoolId, classId, startDate, endDate);
                return build("ledger", "School Ledger Report", table.headers(), table.rows(), format);
        }

        public ReportFile exportPayments(String schoolId, String format, String classId, LocalDate startDate,
                        LocalDate endDate) {
                var page = listStudentPaymentBySchoolUseCase.execute(schoolId, 0, 0);
                List<PaymentDTO> records = page.getContent();

                List<String> headers = List.of("Paid At", "Student", "Class", "Fee Category", "Term", "Amount",
                                "Method",
                                "Reference");
                List<List<String>> rows = records.stream()
                                .filter(r -> classId == null
                                                || (r.student() != null && classId.equals(r.student().classId())))
                                .filter(r -> inRange(toLocalDate(r.paidAt()), startDate, endDate))
                                .map(r -> List.of(
                                                formatInstant(r.paidAt()),
                                                r.student().givenNames() + " " + r.student().familyName(),
                                                safe(r.student().className()),
                                                safe(r.fee().category().name()),
                                                safe(r.fee().term().name()),
                                                safe(r.amount()),
                                                safe(r.paymentMethod()),
                                                safe(r.referenceNo())))
                                .toList();

                return build("payments", "Payments Report", headers, rows, format);
        }

        public ReportFile exportAttendance(String schoolId, String classSessionId, String format, LocalDate startDate,
                        LocalDate endDate) {
                var page = attendanceReportUseCase.execute(schoolId, classSessionId, 0, 0);
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
                var page = listBehaviourBySchoolUseCase.execute(schoolId, classId, 0, 0);
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

        public ReportFile exportStudents(String schoolId, String format, String classId, LocalDate startDate,
                        LocalDate endDate) {
                // var page = listStudentBySchoolUseCase.execute(schoolId, 0, 0);
                // List<StudentDTO> records = page.getContent();

                // List<String> headers = List.of("Admission No", "Given Names", "Family Name",
                // "Gender", "Class",
                // "Status",
                // "Created At");
                // List<List<String>> rows = records.stream()
                // .filter(r -> classId == null || classId.equals(r.classId()))
                // .filter(r -> inRange(toLocalDate(r.createdAt()), startDate, endDate))
                // .map(r -> List.of(
                // safe(r.admissionNumber()),
                // safe(r.givenNames()),
                // safe(r.familyName()),
                // safe(r.gender()),
                // safe(r.className()),
                // safe(r.status()),
                // formatInstant(r.createdAt())))
                // .toList();

                // return build("students", "Students Report", headers, rows, format);
                return null;
        }

        public ReportFile exportFees(String schoolId, String format, String classId, LocalDate startDate,
                        LocalDate endDate) {
                var page = listFeeStructureBySchoolUseCase.execute(schoolId, 0, 0);
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

        public Map<String, Object> runReport(String schoolId, RunReportDTO param, int page, int size) {
                String type = normalizeType(param.entity());
                int limit = size > 0 ? Math.min(size, 200) : 50;
                List<Filter> filters = param.filters().stream().map(
                                e -> new Filter(e.field(), e.operator(), e.type(), e.value(), e.valueTo(), e.values()))
                                .toList();
                var report = new ReportBuilderDTO(schoolId, param.entity(), filters);

                return switch (type) {
                        case "students" -> buildStudentsTable(report, page, limit);
                        case "teachers" -> buildTeachersTable(report, page, size);
                        case "classes" -> buildClassesTable(report, page, size);
                        case "subjects" -> buildSubjectTable(report, page, size);
                        case "attendances" -> buildAttendanceTable(report, page, size);
                        case "grades" -> buildGradesTable(report, page, size);
                        default -> throw new NotFoundException("Unsupported report type");
                };
        }

        private ReportTableDTO buildLedgerTable(String schoolId, String classId, LocalDate startDate,
                        LocalDate endDate) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                var entries = ledgerRepo.findAllBySchoolIdOrderByPaidAtDesc(schoolId, Pageable.unpaged()).getContent();
                List<StudentLedgerDTO> records = new ArrayList<>();

                for (StudentLedgerEntry entry : entries) {
                        var enrollment = enrollmentRepo
                                        .findByStudentAndAcademicYearAndSchoolId(entry.getStudentId(),
                                                        academicYear.getId(), schoolId)
                                        .orElse(null);

                        String studentName = enrollment != null ? enrollment.getStudent().getName() : "Unknown Student";
                        String className = enrollment != null ? enrollment.getClazz().getName() : "Unknown Class";

                        if (classId != null && enrollment != null && !classId.equals(enrollment.getClazz().getId())) {
                                continue;
                        }

                        if (classId != null && enrollment == null) {
                                continue;
                        }

                        if (!inRange(entry.getDate(), startDate, endDate)) {
                                continue;
                        }

                        // records.add(new StudentLedgerDTO(
                        // entry.getDate(),
                        // entry.getTransactionType().name(),
                        // studentName,
                        // className,
                        // entry.getDescription(),
                        // entry.getDebit(),
                        // entry.getCredit(),
                        // entry.getBalance()));
                }

                List<String> headers = List.of("Date", "Type", "Student", "Class", "Description", "Debit", "Credit",
                                "Balance");
                List<List<String>> rows = records.stream()
                                .map(r -> List.of(
                                                formatDate(r.date()),
                                                safe(r.type()),
                                                safe(r.student()),
                                                safe(r.clazz()),
                                                safe(r.description()),
                                                safe(r.debit()),
                                                safe(r.credit()),
                                                safe(r.balance())))
                                .toList();

                return new ReportTableDTO(headers, rows);
        }

        private Map<String, Object> buildStudentsTable(ReportBuilderDTO param, int page, int size) {
                var res = studentReportUseCase.execute(param, page, size);
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());
                var data = res.getContent();
                return Map.of(
                                "data", data,
                                "meta", meta);
        }

        private Map<String, Object> buildGradesTable(ReportBuilderDTO param, int page, int size) {
                var res = gradeReportUseCase.execute(param, page, size);
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());
                var data = res.getContent();
                return Map.of(
                                "data", data,
                                "meta", meta);
        }

        private Map<String, Object> buildTeachersTable(ReportBuilderDTO param, int page, int size) {
                var res = teacherReportUseCase.execute(param, page, size);
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());
                var data = res.getContent();
                return Map.of(
                                "data", data,
                                "meta", meta);
        }

        private Map<String, Object> buildClassesTable(ReportBuilderDTO param, int page, int size) {
                var res = classReportUseCase.execute(param, page, size);
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());
                var data = res.getContent();
                return Map.of(
                                "data", data,
                                "meta", meta);
        }

        private Map<String, Object> buildSubjectTable(ReportBuilderDTO param, int page, int size) {
                var res = subjectReportUseCase.execute(param, page, size);
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());
                var data = res.getContent();
                return Map.of(
                                "data", data,
                                "meta", meta);
        }

        private Map<String, Object> buildAttendanceTable(ReportBuilderDTO param, int page, int size) {
                var res = attendanceReportUseCase.execute(param, page, size);
                Map<String, Object> meta = Map.of(
                                "page", res.getNumber() + 1,
                                "size", res.getSize(),
                                "count", res.getTotalElements(),
                                "pages", res.getTotalPages());
                var data = res.getContent();
                return Map.of(
                                "data", data,
                                "meta", meta);
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
}
