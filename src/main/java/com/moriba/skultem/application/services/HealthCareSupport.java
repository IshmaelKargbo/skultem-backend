package com.moriba.skultem.application.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.NotificationMapper;
import com.moriba.skultem.application.usecase.ResolveTimingForLevelUseCase;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.ParentNotified;
import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.HealthCareSettingRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.repository.NotificationRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.HealthCareLimits;
import com.moriba.skultem.domain.service.HealthCareLimits.TermDates;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.Priority;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What every Health Care use case needs (docs/HEALTH_CARE.md): the school's settings (defaults until it saves its
 * own), the student as they are now, when their school day ends, the current term, their counted visits, and
 * sending notifications to parents and principal-level staff. The clock and user names come from
 * LibraryAttendanceSupport, which is about the school, not the library.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCareSupport {

    /** Principal-level roles: they get Health Care alerts (§6.5). */
    public static final Set<Role> PRINCIPAL_LEVEL = EnumSet.of(Role.PRINCIPAL, Role.OWNER, Role.PROPRIETOR,
            Role.SUPER_ADMIN);

    private final HealthCareSettingRepository settingRepo;
    private final HealthCareVisitRepository visitRepo;
    private final StudentRepository studentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final TermRepository termRepo;
    private final StudentParentRepository studentParentRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationService notificationService;
    private final ResolveTimingForLevelUseCase resolveTimingUseCase;
    private final LibraryAttendanceSupport clock;

    /** A student who can be seen today: in this school and actively enrolled in the active academic year. */
    public record CurrentStudent(Student student, Enrollment enrollment, String className) {
    }

    public Instant now() {
        return clock.now();
    }

    public LocalDate today() {
        return clock.today();
    }

    public String userName(String userId) {
        return clock.userName(userId);
    }

    /** The school's settings, or the defaults (not saved) until a principal saves its own (S-1). */
    public HealthCareSetting settings(String schoolId) {
        return settingRepo.findBySchool(schoolId).orElseGet(() -> HealthCareSetting.defaults(schoolId));
    }

    public CurrentStudent currentStudent(String schoolId, String studentId) {
        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        var year = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("No active academic year found"));
        var enrollment = enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(studentId, year.getId(), schoolId)
                .orElse(null);
        if (student.getStatus() != Student.Status.ACTIVE || enrollment == null
                || enrollment.getStatus() != Enrollment.Status.ACTIVE) {
            throw new RuleException(student.getName() + " is not currently enrolled");
        }
        return new CurrentStudent(student, enrollment, LibraryAttendanceSupport.className(enrollment));
    }

    /**
     * When the student's school day ends today: the end time of the timing for their class's level. Arriving after
     * that (or with no timing set up), they have until midnight. Same rule as library visits.
     */
    public Instant schoolDayEnd(String schoolId, Enrollment enrollment, Instant now) {
        LocalDate today = now.atZone(SchoolTimeZone.ZONE).toLocalDate();
        Instant midnight = today.plusDays(1).atStartOfDay(SchoolTimeZone.ZONE).toInstant();
        try {
            var level = enrollment.getClazz() == null ? null : enrollment.getClazz().getLevel();
            LocalTime end = resolveTimingUseCase.execute(schoolId, level).getEndTime();
            if (end == null) {
                return midnight;
            }
            Instant schoolEnd = ZonedDateTime.of(today, end, SchoolTimeZone.ZONE).toInstant();
            return schoolEnd.isAfter(now) ? schoolEnd : midnight;
        } catch (NotFoundException e) {
            return midnight;
        }
    }

    /** The term {@code day} falls in, in the active academic year, or null (term rules then don't apply - L-2). */
    public TermDates termOn(String schoolId, LocalDate day) {
        var year = academicYearRepo.findActiveBySchool(schoolId).orElse(null);
        if (year == null) {
            return null;
        }
        return termRepo.findByAcademicYearIdAndSchool(year.getId(), schoolId).stream()
                .filter(t -> t.getStartDate() != null && t.getEndDate() != null)
                .filter(t -> !day.isBefore(t.getStartDate()) && !day.isAfter(t.getEndDate()))
                .findFirst()
                .map(t -> new TermDates(t.getStartDate(), t.getEndDate()))
                .orElse(null);
    }

    /**
     * The earliest day any period can reach back to from {@code today}: the start of the term or of the month,
     * whichever is earlier. Loading the student's visits from there covers every limit rule.
     */
    public Instant countingSince(LocalDate today, TermDates term) {
        LocalDate from = today.withDayOfMonth(1).isBefore(today.minusDays(6)) ? today.withDayOfMonth(1)
                : today.minusDays(6);
        if (term != null && term.start().isBefore(from)) {
            from = term.start();
        }
        return from.atStartOfDay(SchoolTimeZone.ZONE).toInstant();
    }

    /** Arrival times of the student's visits that count towards limits (L-1, L-3), since {@code since}. */
    public List<Instant> countedArrivals(String schoolId, String studentId, Instant since) {
        return visitRepo.findByStudentSince(schoolId, studentId, since).stream()
                .filter(HealthCareVisit::countsTowardsLimits)
                .map(HealthCareVisit::getArrivedAt)
                .toList();
    }

    public HealthCareLimits.Check checkLimits(HealthCareSetting setting, CurrentStudent s, boolean urgent,
            List<Instant> counted, LocalDate today, TermDates term) {
        var level = s.enrollment().getClazz() == null ? null : s.enrollment().getClazz().getLevel();
        return HealthCareLimits.check(setting, level, urgent, counted, today, term);
    }

    /** The last time this medicine (by name, ignoring case) was given to the student within the window (M-2). */
    public java.util.Optional<HealthCareVisit.Medication> lastSameMedicine(String schoolId, String studentId,
            String name, int hours, Instant now) {
        if (name == null || name.isBlank()) {
            return java.util.Optional.empty();
        }
        Instant since = now.minus(java.time.Duration.ofHours(hours));
        String wanted = name.trim().toLowerCase(java.util.Locale.ROOT);
        // Visits from the day before can hold doses inside the window too.
        return visitRepo.findByStudentSince(schoolId, studentId, since.minus(java.time.Duration.ofDays(1))).stream()
                .filter(v -> !v.isVoided())
                .flatMap(v -> v.getMedications().stream())
                .filter(m -> m.name() != null && m.name().trim().toLowerCase(java.util.Locale.ROOT).equals(wanted))
                .filter(m -> !m.givenAt().isBefore(since))
                .max(java.util.Comparator.comparing(HealthCareVisit.Medication::givenAt));
    }

    /** The outcome of telling parents: who was reached, and NONE / FAILED / null as the problem. */
    public record Notified(List<ParentNotified> parents, String problem) {
    }

    /**
     * Tells each linked parent with a user account, once each (N-1 to N-6). Sending happens after the visit is saved;
     * a failure for one parent never stops the visit or the others.
     */
    public Notified notifyParents(HealthCareVisit visit, boolean manual, Instant now) {
        Set<String> seen = new HashSet<>();
        List<User> parents = new ArrayList<>();
        for (var link : studentParentRepo.findAllByStudentAndSchool(visit.getStudentId(), visit.getSchoolId())) {
            var parent = link.getParent();
            if (parent != null && parent.getUser() != null && seen.add(parent.getUser().getId())) {
                parents.add(parent.getUser());
            }
        }
        if (parents.isEmpty()) {
            return new Notified(List.of(), "NONE");
        }

        String first = firstName(visit.getStudentName());
        String title = switch (visit.getOutcome() == null ? HealthCareVisit.Outcome.RETURNED_TO_CLASS : visit.getOutcome()) {
            case SENT_HOME -> first + " was sent home";
            case REFERRED -> first + " was referred to hospital";
            case RETURNED_TO_CLASS -> first + " visited the sick bay";
        };
        String message = parentMessage(visit);
        Map<String, String> meta = Map.of(
                "student_id", visit.getStudentId(),
                "student_name", visit.getStudentName(),
                "visit_id", visit.getId(),
                "outcome", visit.getOutcome() == null ? "" : visit.getOutcome().name());

        List<ParentNotified> reached = new ArrayList<>();
        boolean failed = false;
        for (User user : parents) {
            if (send(visit.getSchoolId(), user, title, message, meta)) {
                reached.add(new ParentNotified(user.getId(), user.getName(), now, manual));
            } else {
                failed = true;
            }
        }
        return new Notified(reached, failed ? "FAILED" : null);
    }

    /** N-3: when, what was wrong, how it ended, and where to for a referral - no notes, medication or temperature. */
    private static String parentMessage(HealthCareVisit visit) {
        var time = visit.getArrivedAt().atZone(SchoolTimeZone.ZONE);
        String when = String.format("%s at %02d:%02d", time.toLocalDate(), time.getHour(), time.getMinute());
        String complaint = String.join(", ", visit.getComplaints().stream().map(HealthCareVisit.Complaint::label).toList());
        if (visit.getComplaintOther() != null) {
            complaint = complaint + " (" + visit.getComplaintOther() + ")";
        }
        String outcome = switch (visit.getOutcome() == null ? HealthCareVisit.Outcome.RETURNED_TO_CLASS : visit.getOutcome()) {
            case SENT_HOME -> "They were sent home" + (visit.getCollectedBy() == null ? "."
                    : ", collected by " + visit.getCollectedBy() + ".");
            case REFERRED -> "They were referred to " + visit.getReferredTo() + ".";
            case RETURNED_TO_CLASS -> "They were seen and returned to class.";
        };
        return visit.getStudentName() + " came to the sick bay on " + when + " with: " + complaint + ". " + outcome
                + " Please contact the school if you have any questions.";
    }

    /** Tells every principal-level user of the school (N-0). Never fails the action that triggered it. */
    public void notifyPrincipals(String schoolId, String title, String message, Map<String, String> meta) {
        Set<String> seen = new HashSet<>();
        for (SchoolUser su : schoolUserRepo.findBySchool(schoolId)) {
            if (su.getUser() != null && PRINCIPAL_LEVEL.contains(su.getRole())
                    && su.getStatus() != SchoolUser.Status.INACTIVE && seen.add(su.getUser().getId())) {
                send(schoolId, su.getUser(), title, message, meta);
            }
        }
    }

    private boolean send(String schoolId, User user, String title, String message, Map<String, String> meta) {
        try {
            var notification = Notification.create(UUID.randomUUID().toString(), schoolId, user,
                    Notification.Type.HEALTH, title, message, meta, Priority.HIGH);
            notificationRepo.save(notification);
            long count = notificationRepo.countAllOpenByOwnerAndSchoolId(user.getId(), schoolId);
            notificationService.sendToUser(user.getId(), count, NotificationMapper.toDTO(notification));
            return true;
        } catch (RuntimeException e) {
            log.warn("Health care notification to user {} failed: {}", user.getId(), e.getMessage());
            return false;
        }
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "Your child";
        }
        return name.trim().split("\\s+")[0];
    }
}
