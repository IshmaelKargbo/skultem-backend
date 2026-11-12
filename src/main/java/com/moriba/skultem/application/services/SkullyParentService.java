package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.dto.StudentTermAttendanceDetailDTO;
import com.moriba.skultem.application.usecase.GetStudentTermAttendanceUseCase;
import com.moriba.skultem.application.usecase.ListStudentReportCardsUseCase;
import com.moriba.skultem.domain.model.FeeDiscount.Kind;
import com.moriba.skultem.domain.model.Notice;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.repository.CalendarEventRepository;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.domain.repository.NoticeRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Audience;
import com.moriba.skultem.domain.vo.FeatureModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What Skully, the parents' WhatsApp assistant, may tell a parent. Everything is looked up from the WhatsApp number
 * the message came from, so a parent only ever hears about their own children and their schools: a student or school
 * that is not theirs is simply not found.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkullyParentService {

    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final SchoolRepository schoolRepo;
    private final StudentFeeRepository studentFeeRepo;
    private final FeeDiscountRepository discountRepo;
    private final PaymentRepository paymentRepo;
    private final NoticeRepository noticeRepo;
    private final CalendarEventRepository calendarRepo;
    private final GetStudentTermAttendanceUseCase attendanceUseCase;
    private final ListStudentReportCardsUseCase reportCardsUseCase;
    private final ModuleAccessService modules;

    // The country code a phone saved without one (077 123 456) is in.
    @Value("${skully.default-country-code:232}")
    private String defaultCountryCode;

    public record Child(String id, String name, String admissionNumber, String className, String status) {
    }

    public record SchoolFamily(String schoolId, String schoolName, String schoolPhone, String parentName,
            List<Child> children) {
    }

    // schools: where Skully may answer. notEnabledAt: schools this parent is at that have not installed Skully.
    public record Family(String phone, List<SchoolFamily> schools, List<String> notEnabledAt) {
    }

    public record FeeLine(String item, String term, String academicYear, BigDecimal amount, BigDecimal discount,
            BigDecimal paid, BigDecimal outstanding, LocalDate dueDate) {
    }

    public record Fees(BigDecimal totalBilled, BigDecimal totalPaid, BigDecimal totalOutstanding, List<FeeLine> items) {
    }

    public record ReportCard(String term, String academicYear, String className, double average, int position,
            String overallGrade, boolean passed, String remark) {
    }

    public record ChildSummary(Child child, String schoolName, StudentTermAttendanceDetailDTO attendance, Fees fees,
            List<ReportCard> reportCards) {
    }

    public record Update(String kind, String title, String body, String category, Instant at, Instant endsAt,
            String location) {
    }

    // ---------------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Family family(String phone) {
        List<SchoolFamily> schools = new ArrayList<>();
        List<String> notEnabledAt = new ArrayList<>();
        for (Parent parent : allParents(phone)) {
            if (!skullyOn(parent.getSchoolId())) {
                schoolRepo.findById(parent.getSchoolId()).ifPresent(s -> notEnabledAt.add(s.getName()));
                continue;
            }
            var school = schoolRepo.findById(parent.getSchoolId());
            if (school.isEmpty()) continue;
            var children = studentRepo.findByParentAndSchoolId(parent.getId(), parent.getSchoolId(), PageRequest.of(0, 50))
                    .getContent().stream()
                    .filter(s -> s.getStatus() == Student.Status.ACTIVE)
                    .map(this::child)
                    .toList();
            var user = parent.getUser();
            String name = user == null ? null : (nz(user.getGivenNames()) + " " + nz(user.getFamilyName())).trim();
            schools.add(new SchoolFamily(parent.getSchoolId(), school.get().getName(), school.get().getPhone(), name,
                    children));
        }
        return new Family(phone, schools, notEnabledAt);
    }

    @Transactional(readOnly = true)
    public Optional<ChildSummary> child(String phone, String studentId) {
        return ownChild(phone, studentId).map(student -> {
            String school = student.getSchoolId();
            StudentTermAttendanceDetailDTO attendance = null;
            try {
                attendance = attendanceUseCase.executeForStudent(school, studentId, null, null);
            } catch (RuntimeException e) {
                log.debug("Skully: no attendance for {}: {}", studentId, e.getMessage());
            }
            List<ReportCard> cards = List.of();
            try {
                cards = reportCardsUseCase.execute(school, studentId, true).stream()
                        .limit(3).map(SkullyParentService::card).toList();
            } catch (RuntimeException e) {
                log.debug("Skully: no report cards for {}: {}", studentId, e.getMessage());
            }
            String schoolName = schoolRepo.findById(school).map(s -> s.getName()).orElse(null);
            return new ChildSummary(child(student), schoolName, attendance, fees(student), cards);
        });
    }

    // The school's current notices for parents and its upcoming events (whole-school ones only).
    @Transactional(readOnly = true)
    public Optional<List<Update>> updates(String phone, String schoolId) {
        boolean theirs = parents(phone).stream().anyMatch(p -> p.getSchoolId().equals(schoolId));
        if (!theirs) return Optional.empty();
        Instant now = Instant.now();
        List<Update> out = new ArrayList<>();
        noticeRepo.findAllBySchoolId(schoolId, PageRequest.of(0, 30)).getContent().stream()
                .filter(n -> n.getManagementSectionId() == null)
                .filter(n -> n.getAudience() == Audience.ALL || n.getAudience() == Audience.PARENTS)
                .filter(n -> n.getExpiresAt() == null || n.getExpiresAt().isAfter(now))
                .limit(8)
                .forEach(n -> out.add(notice(n)));
        calendarRepo.findUpcomingSchoolWide(schoolId, now.minusSeconds(12 * 3600), 8).forEach(e -> out.add(
                new Update("event", e.getTitle(), e.getDescription(), e.getType() == null ? null : e.getType().name(),
                        e.getStartDate(), e.getEndDate(), e.getLocation())));
        return Optional.of(out);
    }

    // ---------------------------------------------------------------------------------------------------------

    // The parent records Skully may use: at schools that installed the WhatsApp Assistant module.
    private List<Parent> parents(String phone) {
        return allParents(phone).stream().filter(p -> skullyOn(p.getSchoolId())).toList();
    }

    private List<Parent> allParents(String phone) {
        return parentRepo.findAllByPhoneDigits(phoneForms(phone)).stream()
                .filter(p -> p.getStatus() == null || !"DELETED".equals(p.getStatus().name()))
                .toList();
    }

    private boolean skullyOn(String schoolId) {
        return modules.isEnabled(schoolId, FeatureModule.WHATSAPP_ASSISTANT);
    }

    private Optional<Student> ownChild(String phone, String studentId) {
        for (Parent parent : parents(phone)) {
            var student = studentRepo.findByIdAndSchoolId(studentId, parent.getSchoolId());
            if (student.isPresent() && student.get().getParent() != null
                    && parent.getId().equals(student.get().getParent().getId())) {
                return student;
            }
        }
        return Optional.empty();
    }

    // The ways a number may have been saved, digits only: 23277123456, and 077123456 when it is a local number.
    Set<String> phoneForms(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        Set<String> forms = new LinkedHashSet<>();
        if (digits.length() < 7) return forms;
        forms.add(digits);
        String code = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("[^0-9]", "");
        if (!code.isEmpty() && digits.startsWith(code) && digits.length() > code.length() + 5) {
            String local = digits.substring(code.length());
            forms.add(local);
            forms.add("0" + local);
        }
        if (digits.startsWith("00")) forms.add(digits.substring(2));
        return forms;
    }

    private Child child(Student s) {
        String className = null;
        try {
            className = s.getSession() == null ? s.getLastClass() : s.getSession().getName();
        } catch (RuntimeException ignored) {
            className = s.getLastClass();
        }
        return new Child(s.getId(), (nz(s.getGivenNames()) + " " + nz(s.getFamilyName())).trim(), s.getAdmissionNumber(),
                className, s.getStatus() == null ? null : s.getStatus().name());
    }

    private Fees fees(Student student) {
        List<FeeLine> lines = new ArrayList<>();
        BigDecimal billed = BigDecimal.ZERO, paidAll = BigDecimal.ZERO, owed = BigDecimal.ZERO;
        List<StudentFee> rows = studentFeeRepo
                .findAllBySchoolAndStudent(student.getSchoolId(), student.getId(), PageRequest.of(0, 100)).getContent();
        for (StudentFee row : rows) {
            var fee = row.getFee();
            if (fee == null || fee.getAmount() == null) continue;
            BigDecimal discount = discountRepo.findBySchoolAndStudentIdAndFeeId(fee.getSchoolId(), student.getId(),
                    fee.getId()).stream()
                    .map(d -> d.getKind() == Kind.PERCENTAGE
                            ? fee.getAmount().multiply(d.getValue()).divide(BigDecimal.valueOf(100), 2,
                                    RoundingMode.HALF_UP)
                            : d.getValue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paid = Optional.ofNullable(paymentRepo.sumPaymentsByStudentAndFee(student.getId(), fee.getId()))
                    .orElse(BigDecimal.ZERO);
            BigDecimal outstanding = fee.getAmount().subtract(discount).subtract(paid).max(BigDecimal.ZERO);
            billed = billed.add(fee.getAmount().subtract(discount));
            paidAll = paidAll.add(paid);
            owed = owed.add(outstanding);
            lines.add(new FeeLine(
                    fee.getCategory() == null ? fee.getDescription() : fee.getCategory().getName(),
                    fee.getTerm() == null ? null : fee.getTerm().getName(),
                    fee.getAcademicYear() == null ? null : fee.getAcademicYear().getName(),
                    fee.getAmount(), discount, paid, outstanding, fee.getDueDate()));
        }
        return new Fees(billed, paidAll, owed, lines);
    }

    private static ReportCard card(ReportCardSummaryDTO c) {
        return new ReportCard(c.termName(), c.academicYearName(), c.className(), c.average(), c.position(),
                c.overallGrade(), c.passed(), null);
    }

    private static Update notice(Notice n) {
        return new Update("notice", n.getTitle(), n.getContent(), n.getCategory() == null ? null : n.getCategory().name(),
                n.getEventAt() != null ? n.getEventAt() : n.getCreatedAt(), n.getEventEndsAt(), n.getEventLocation());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
