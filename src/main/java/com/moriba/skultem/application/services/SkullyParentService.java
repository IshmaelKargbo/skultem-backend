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
import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.LibraryLoan;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.application.usecase.LogActivityUseCase;

import org.springframework.jdbc.core.JdbcTemplate;

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
    private final LogActivityUseCase activity;
    private final LibraryLoanRepository loanRepo;
    private final HealthCareVisitRepository visitRepo;
    private final BehaviourRepository behaviourRepo;
    private final JdbcTemplate jdbc;

    // The optional modules Skully can also report on, when the school has installed them.
    private static final List<FeatureModule> EXTRAS = List.of(FeatureModule.LIBRARY, FeatureModule.HEALTH_CARE,
            FeatureModule.BEHAVIOUR);

    // The country code a phone saved without one (077 123 456) is in.
    @Value("${skully.default-country-code:232}")
    private String defaultCountryCode;

    // Where each school's portal is, {domain} being the school's subdomain (same setting as the emails use).
    @Value("${app.portal-url:https://{domain}.skultem.space}")
    private String portalUrl;

    public record Child(String id, String name, String admissionNumber, String className, String status) {
    }

    // features: the optional modules (library, health-care, behaviour) this school uses, so Skully knows what to offer.
    public record SchoolFamily(String schoolId, String schoolName, String schoolPhone, String parentName,
            List<Child> children, List<String> features) {
    }

    // schools: where Skully may answer. notEnabledAt: schools this parent is at that have not installed Skully.
    /** staff: schools where this number is staff (SkullyStaffService), so Skully can answer them about the school. */
    public record Family(String phone, List<SchoolFamily> schools, List<String> notEnabledAt,
            List<SkullyStaffService.StaffAccess> staff) {

        public Family withStaff(List<SkullyStaffService.StaffAccess> staff) {
            return new Family(phone, schools, notEnabledAt, staff);
        }
    }

    public record FeeLine(String item, String term, String academicYear, BigDecimal amount, BigDecimal discount,
            BigDecimal paid, BigDecimal outstanding, LocalDate dueDate) {
    }

    public record Fees(BigDecimal totalBilled, BigDecimal totalPaid, BigDecimal totalOutstanding, List<FeeLine> items) {
    }

    public record ReportCard(String term, String academicYear, String className, double average, int position,
            String overallGrade, boolean passed, String remark) {
    }

    /**
     * The latest term's marks in one subject, once the school approved them (0-100): the child's average over the
     * assessments so far, the latest one, and the class average for the same assessments (no other child's marks).
     */
    public record SubjectMarks(String term, String subject, int average, int assessments, int latest,
            Integer classAverage) {
    }

    // library, sickBay and behaviour are null when the school does not use that module.
    public record ChildSummary(Child child, String schoolName, StudentTermAttendanceDetailDTO attendance, Fees fees,
            List<ReportCard> reportCards, List<SubjectMarks> subjects, List<Loan> library, List<SickBayVisit> sickBay,
            List<BehaviourNote> behaviour) {
    }

    public record Loan(String book, String author, LocalDate issued, LocalDate due, boolean returned,
            boolean overdue) {
    }

    // What parents are told about a sick bay visit (the same as the school's own notice to them): when, what was
    // wrong and how it ended - never nurse's notes, medication or temperature.
    public record SickBayVisit(String when, List<String> complaints, String outcome, String referredTo,
            String collectedBy, boolean stillInSickBay) {
    }

    public record BehaviourNote(LocalDate date, String kind, String category, String note) {
    }

    /**
     * How a parent gets into a school's parent portal. status: PENDING (never signed in - temporaryPassword is
     * set when Skultem still has it), ACTIVE (has their own password), or DISABLED (the school switched it off).
     */
    public record PortalAccess(String schoolId, String schoolName, String loginUrl, String signInWith, String status,
            String temporaryPassword, boolean hasEmail) {
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
            var features = EXTRAS.stream().filter(m -> modules.isEnabled(parent.getSchoolId(), m))
                    .map(FeatureModule::key).toList();
            schools.add(new SchoolFamily(parent.getSchoolId(), school.get().getName(), school.get().getPhone(), name,
                    children, features));
        }
        return new Family(phone, schools, notEnabledAt, List.of());
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
            List<SubjectMarks> subjects = List.of();
            try {
                subjects = subjects(school, studentId);
            } catch (RuntimeException e) {
                log.debug("Skully: no subject marks for {}: {}", studentId, e.getMessage());
            }
            String schoolName = schoolRepo.findById(school).map(s -> s.getName()).orElse(null);
            return new ChildSummary(child(student), schoolName, attendance, fees(student), cards, subjects,
                    modules.isEnabled(school, FeatureModule.LIBRARY) ? loans(student) : null,
                    modules.isEnabled(school, FeatureModule.HEALTH_CARE) ? sickBay(student) : null,
                    modules.isEnabled(school, FeatureModule.BEHAVIOUR) ? behaviour(student) : null);
        });
    }

    // The parent's portal sign-in for each of their schools that uses Skully. Handing out a still-pending account's
    // temporary password is what an admin would do for them; it goes only to the parent's own phone on file, and is
    // written to the school's activity log.
    @Transactional
    public List<PortalAccess> portalAccess(String phone) {
        List<PortalAccess> out = new ArrayList<>();
        for (Parent parent : parents(phone)) {
            var school = schoolRepo.findById(parent.getSchoolId()).orElse(null);
            User user = parent.getUser();
            if (school == null || user == null) continue;
            String status = switch (user.getStatus()) {
                case RESET_PASSWORD -> "PENDING";
                case ACTIVE -> "ACTIVE";
                default -> "DISABLED";
            };
            String temporary = "PENDING".equals(status) && user.getHint() != null && !user.getHint().isBlank()
                    ? user.getHint() : null;
            boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
            String loginUrl = portalUrl.replace("{domain}", nz(school.getDomain())).replaceAll("/+$", "") + "/login";
            out.add(new PortalAccess(school.getId(), school.getName(), loginUrl,
                    hasEmail ? user.getEmail() : parent.getPhone(), status, temporary, hasEmail));
            if (temporary != null) {
                activity.log(school.getId(), ActivityType.USER, "Portal sign-in details sent on WhatsApp (Skully)",
                        (nz(user.getGivenNames()) + " " + nz(user.getFamilyName())).trim(), null, user.getId());
            }
        }
        return out;
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

    private List<Loan> loans(Student student) {
        LocalDate today = LocalDate.now(SchoolTimeZone.ZONE);
        return loanRepo.findRecentByStudent(student.getSchoolId(), student.getId(), 8).stream()
                .filter(l -> l.getStatus() == LibraryLoan.Status.ISSUED
                        || (l.getReturnedAt() != null && l.getReturnedAt().isAfter(Instant.now().minusSeconds(60L * 86400))))
                .map(l -> {
                    boolean returned = l.getStatus() == LibraryLoan.Status.RETURNED;
                    return new Loan(l.getBook() == null ? null : l.getBook().getTitle(),
                            l.getBook() == null ? null : l.getBook().getAuthor(), l.getIssueDate(), l.getDueDate(),
                            returned, !returned && l.getDueDate() != null && l.getDueDate().isBefore(today));
                })
                .toList();
    }

    // The last 90 days' visits (voided ones left out), newest first.
    private List<SickBayVisit> sickBay(Student student) {
        return visitRepo.findByStudentSince(student.getSchoolId(), student.getId(),
                Instant.now().minusSeconds(90L * 86400)).stream()
                .filter(v -> v.getStatus() != HealthCareVisit.Status.VOIDED)
                .sorted(java.util.Comparator.comparing(HealthCareVisit::getArrivedAt).reversed())
                .limit(5)
                .map(v -> {
                    var t = v.getArrivedAt().atZone(SchoolTimeZone.ZONE);
                    String when = String.format("%s %02d:%02d", t.toLocalDate(), t.getHour(), t.getMinute());
                    List<String> complaints = new ArrayList<>(v.getComplaints() == null ? List.of()
                            : v.getComplaints().stream().map(HealthCareVisit.Complaint::label).toList());
                    if (v.getComplaintOther() != null && !v.getComplaintOther().isBlank()) complaints.add(v.getComplaintOther());
                    boolean open = v.getStatus() == HealthCareVisit.Status.OPEN;
                    String outcome = open || v.getOutcome() == null ? null : switch (v.getOutcome()) {
                        case RETURNED_TO_CLASS -> "seen and returned to class";
                        case SENT_HOME -> "sent home";
                        case REFERRED -> "referred to hospital";
                    };
                    return new SickBayVisit(when, complaints, outcome,
                            v.getOutcome() == HealthCareVisit.Outcome.REFERRED ? v.getReferredTo() : null,
                            v.getOutcome() == HealthCareVisit.Outcome.SENT_HOME ? v.getCollectedBy() : null, open);
                })
                .toList();
    }

    private List<BehaviourNote> behaviour(Student student) {
        return behaviourRepo.findRecentByStudent(student.getSchoolId(), student.getId(), 8).stream()
                .map((Behaviour b) -> new BehaviourNote(
                        b.getCreatedAt() == null ? null : b.getCreatedAt().atZone(SchoolTimeZone.ZONE).toLocalDate(),
                        b.getKind() == null ? null : b.getKind().name().toLowerCase(),
                        b.getCategory() == null ? null : b.getCategory().getName(), b.getNote()))
                .toList();
    }

    // The most recent term with approved marks for this child, per subject.
    List<SubjectMarks> subjects(String schoolId, String studentId) {
        return jdbc.query("""
                WITH mine AS (
                    SELECT lc.id AS lc_id, lc.term_id, t.name AS term, t.start_date, s.name AS subject, sc.score,
                           sc.updated_at
                    FROM assessment_scores sc
                    JOIN student_assessments sa ON sa.id = sc.student_assessment_id
                    JOIN enrollments e ON e.id = sa.enrollment_id AND e.student_id = ?
                    JOIN class_subject_assessment_life_cycle lc ON lc.id = sc.class_subject_assessment_life_cycle_id
                        AND lc.status IN ('APPROVED', 'COMPLETED', 'LOCKED')
                    JOIN terms t ON t.id = lc.term_id
                    JOIN teacher_subjects ts ON ts.id = lc.teacher_subject_id
                    JOIN subjects s ON s.id = ts.subject_id
                    WHERE sc.school_id = ?
                ), latest_term AS (SELECT term_id FROM mine ORDER BY start_date DESC LIMIT 1)
                SELECT m.term, m.subject, ROUND(AVG(m.score)) AS average, COUNT(*) AS assessments,
                       (ARRAY_AGG(m.score ORDER BY m.updated_at DESC))[1] AS latest,
                       (SELECT ROUND(AVG(o.score)) FROM assessment_scores o
                        WHERE o.class_subject_assessment_life_cycle_id = ANY (ARRAY_AGG(m.lc_id))) AS class_average
                FROM mine m
                WHERE m.term_id = (SELECT term_id FROM latest_term)
                GROUP BY m.term, m.subject
                ORDER BY m.subject
                """, (rs, n) -> new SubjectMarks(rs.getString("term"), rs.getString("subject"), rs.getInt("average"),
                rs.getInt("assessments"), rs.getInt("latest"),
                rs.getObject("class_average") == null ? null : rs.getInt("class_average")), studentId, schoolId);
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
