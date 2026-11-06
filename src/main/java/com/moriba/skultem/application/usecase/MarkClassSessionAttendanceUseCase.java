package com.moriba.skultem.application.usecase;

import org.springframework.cache.annotation.CacheEvict;
import com.moriba.skultem.config.CacheConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AttendanceMapper;
import com.moriba.skultem.application.mapper.NotificationMapper;
import com.moriba.skultem.application.services.AttendanceRulesResolver;
import com.moriba.skultem.application.services.NotificationService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;
import com.moriba.skultem.domain.repository.NotificationRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.vo.Priority;
import com.moriba.skultem.infrastructure.security.PermissionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Changes what ComputeClassAttentionUseCase reports - clears its cache once a call here succeeds.
@CacheEvict(cacheNames = CacheConfig.CLASS_ATTENTION, allEntries = true)
@Service
@Transactional
@RequiredArgsConstructor
public class MarkClassSessionAttendanceUseCase {
    private final AttendanceRepository attendanceRepo;
    private final ClassSessionRepository classSessionRepo;
    private final AcademicYearRepository academicYearRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationService notificationService;
    private final StudentParentRepository studentParentRepo;
    private final HolidayRepository holidayRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final SchoolRepository schoolRepo;
    private final AttendanceRulesResolver attendanceRulesResolver;

    @AuditLogAnnotation(action = "CLASS_ATTENDANCE_MARKED")
    public List<AttendanceDTO> execute(
            String schoolId,
            String classSessionId,
            LocalDate date,
            boolean holiday,
            List<MarkRecord> records) {
        return execute(schoolId, classSessionId, date, holiday, null, records);
    }

    // roll is which roll call (1 or 2) is being taken. It is only used, and then required, when the
    // class's school/section takes two roll calls a day; otherwise the whole day is marked at once.
    @AuditLogAnnotation(action = "CLASS_ATTENDANCE_MARKED")
    public List<AttendanceDTO> execute(
            String schoolId,
            String classSessionId,
            LocalDate date,
            boolean holiday,
            Integer roll,
            List<MarkRecord> records) {

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("No active academic year found"));

        var classSession = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        Map<String, Enrollment> enrollmentByStudent = loadSessionEnrollments(classSession, schoolId).stream()
                .collect(Collectors.toMap(
                        e -> e.getStudent().getId(),
                        Function.identity(),
                        (a, b) -> a));

        List<LocalDate> schoolHolidays = holidayRepo
                .findAllBySchoolIdAndAcademicYear(schoolId, academicYear.getId(), Pageable.unpaged())
                .getContent()
                .stream()
                .map(h -> h.getDate())
                .toList();

        boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || schoolHolidays.contains(date);

        if (isHoliday && !holiday) {
            throw new RuleException("Cannot mark attendance on a weekend or holiday");
        }

        var school = schoolRepo.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));
        boolean twoRollCalls = attendanceRulesResolver.rollCallsForLevel(school,
                classSession.getClazz().getLevel()) == 2;
        if (twoRollCalls && (roll == null || (roll != 1 && roll != 2))) {
            throw new RuleException("Pick which roll call you are taking (1st or 2nd)");
        }

        String recordedByUserId = PermissionService.getCurrentUser().userId();

        return records.stream().map(record -> {
            var enrollment = enrollmentByStudent.get(record.studentId());
            if (enrollment == null) {
                throw new NotFoundException("Student is not enrolled in this class session");
            }

            validateAttendanceState(record.present(), record.excused(), record.reason(), record.late());

            var parents = findNotificationParents(record.studentId(), schoolId);
            var existingOpt = attendanceRepo.findByEnrollmentAndDateAndSchoolId(enrollment.getId(), date, schoolId);

            Attendance attendance;
            if (twoRollCalls) {
                var mark = Attendance.RollMark.of(record.present(), record.excused(), record.late());
                if (existingOpt.isPresent()) {
                    attendance = existingOpt.get();
                } else {
                    // Starts as the first mark taken; markRoll below sets the day from it.
                    attendance = Attendance.create(UUID.randomUUID().toString(), schoolId, enrollment, date,
                            false, false, false, null, holiday, recordedByUserId);
                }
                var prevMark = attendance.rollMark(roll);

                attendance.markRoll(roll, mark, record.reason(), holiday, recordedByUserId);
                attendanceRepo.save(attendance);

                // Parents hear after each roll call, so a missed morning is known the same day.
                if (mark != prevMark) {
                    parents.forEach(parent -> notifyParentForRollCall(attendance, parent, roll, mark,
                            prevMark != null));
                }
            } else if (existingOpt.isPresent()) {
                attendance = existingOpt.get();

                String prevStatus = attendance.getStatus();

                attendance.update(record.present(), record.excused(), record.late(), record.reason(), holiday,
                        recordedByUserId);
                attendanceRepo.save(attendance);

                parents.forEach(parent -> notifyParentForAttendanceChange(attendance, parent, prevStatus));
            } else {
                var id = UUID.randomUUID().toString();
                attendance = Attendance.create(id, schoolId, enrollment, date, record.present(), record.excused(),
                        record.late(), record.reason(), holiday, recordedByUserId);
                attendanceRepo.save(attendance);

                parents.forEach(parent -> notifyParentForAttendanceChange(attendance, parent, null));
            }

            return AttendanceMapper.toDTO(attendance);
        }).toList();
    }

    private List<Parent> findNotificationParents(String studentId, String schoolId) {
        Set<String> seenUserIds = new HashSet<>();
        List<Parent> parents = studentParentRepo.findAllByStudentAndSchool(studentId, schoolId).stream()
                .map(studentParent -> studentParent.getParent())
                .filter(parent -> parent != null && parent.getUser() != null)
                .filter(parent -> seenUserIds.add(parent.getUser().getId()))
                .toList();

        if (parents.isEmpty()) {
            throw new NotFoundException("no parent relation found");
        }

        return parents;
    }

    private void notifyParentForAttendanceChange(Attendance attendance, Parent parent, String prevStatus) {
        if (parent == null)
            return;

        String status = attendance.getStatus();
        if (status.equals(prevStatus) || !isParentAttendanceNotificationStatus(status)) {
            return;
        }

        Map<String, String> meta = Map.of(
                "student_id", attendance.getEnrollment().getStudent().getId(),
                "student_name", attendance.getEnrollment().getStudent().getName(),
                "attendance_date", attendance.getDate().toString(),
                "status", status,
                "prev_status", prevStatus != null ? prevStatus : "",
                "corrected", prevStatus != null ? "true" : "false");

        String title = "Attendance Update for " + attendance.getEnrollment().getStudent().getName() + " on "
                + attendance.getDate();
        String message;

        if ("Late".equals(status)) {
            message = "Your child was marked Late for " + attendance.getDate();
        } else if ("Absent".equals(status)) {
            message = "Your child was marked Absent for " + attendance.getDate();
        } else {
            message = "Your child was marked Excused for " + attendance.getDate();
        }

        Notification notification = Notification.create(
                UUID.randomUUID().toString(),
                attendance.getSchoolId(),
                parent.getUser(),
                Notification.Type.ATTENDANCE,
                title,
                message,
                meta,
                Priority.HIGH
            );

        notificationRepo.save(notification);
        long count = notificationRepo.countAllOpenByOwnerAndSchoolId(parent.getUser().getId(), attendance.getSchoolId());
        notificationService.sendToUser(parent.getUser().getId(), count, NotificationMapper.toDTO(notification));
    }

    private void notifyParentForRollCall(Attendance attendance, Parent parent, int roll, Attendance.RollMark mark,
            boolean corrected) {
        if (parent == null || mark == Attendance.RollMark.PRESENT) {
            return;
        }

        String rollName = roll == 1 ? "1st" : "2nd";
        String label = switch (mark) {
            case LATE -> "Late";
            case EXCUSED -> "Excused";
            default -> "Absent";
        };
        String name = attendance.getEnrollment().getStudent().getName();

        Map<String, String> meta = Map.of(
                "student_id", attendance.getEnrollment().getStudent().getId(),
                "student_name", name,
                "attendance_date", attendance.getDate().toString(),
                "status", label,
                "roll_call", String.valueOf(roll),
                "day_status", attendance.getStatus(),
                "corrected", corrected ? "true" : "false");

        String title = "Attendance Update for " + name + " on " + attendance.getDate();
        String message = "Your child was marked " + label + " at the " + rollName + " roll call on "
                + attendance.getDate() + ".";
        if (mark == Attendance.RollMark.ABSENT) {
            message += " A student must be present at both roll calls to count as present for the day.";
        }

        Notification notification = Notification.create(
                UUID.randomUUID().toString(),
                attendance.getSchoolId(),
                parent.getUser(),
                Notification.Type.ATTENDANCE,
                title,
                message,
                meta,
                Priority.HIGH);

        notificationRepo.save(notification);
        long count = notificationRepo.countAllOpenByOwnerAndSchoolId(parent.getUser().getId(), attendance.getSchoolId());
        notificationService.sendToUser(parent.getUser().getId(), count, NotificationMapper.toDTO(notification));
    }

    private boolean isParentAttendanceNotificationStatus(String status) {
        return "Late".equals(status) || "Absent".equals(status) || "Excused".equals(status);
    }

    private List<Enrollment> loadSessionEnrollments(ClassSession classSession, String schoolId) {
        return enrollmentRepo.findAllByClassAndAcademicAndSchoolId(
                classSession.getClazz().getId(),
                classSession.getAcademicYear().getId(),
                schoolId, Pageable.unpaged()).stream().filter(enrollment -> {
                    boolean sectionMatch = enrollment.getSection() != null
                            && classSession.getSection() != null
                            && enrollment.getSection().getId().equals(classSession.getSection().getId());

                    if (!sectionMatch) {
                        return false;
                    }

                    if (classSession.getStream() == null) {
                        return enrollment.getStream() == null;
                    }

                    return enrollment.getStream() != null
                            && enrollment.getStream().getId().equals(classSession.getStream().getId());
                }).toList();
    }

    private void validateAttendanceState(boolean present, boolean excused, String reason, boolean late) {
        if (present && excused) {
            throw new RuleException("Present attendance cannot be marked as excused");
        }

        if (present && late) {
            throw new RuleException("Present attendance cannot be marked as late");
        }

        if (!present && excused && (reason == null || reason.isBlank())) {
            throw new RuleException("Reason is required for an unexcused absence");
        }
    }

    public record MarkRecord(
            String studentId,
            boolean present,
            boolean excused,
            boolean late,
            String reason) {
    }
}
