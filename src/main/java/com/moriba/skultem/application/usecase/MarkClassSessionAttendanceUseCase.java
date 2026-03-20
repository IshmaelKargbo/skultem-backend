package com.moriba.skultem.application.usecase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AttendanceMapper;
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
import com.moriba.skultem.domain.vo.Priority;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MarkClassSessionAttendanceUseCase {
    private final AttendanceRepository attendanceRepo;
    private final ClassSessionRepository classSessionRepo;
    private final AcademicYearRepository academicYearRepo;
    private final NotificationRepository notificationRepo;
    private final HolidayRepository holidayRepo;
    private final EnrollmentRepository enrollmentRepo;

    @AuditLogAnnotation(action = "CLASS_ATTENDANCE_MARKED")
    public List<AttendanceDTO> execute(
            String schoolId,
            String classSessionId,
            LocalDate date,
            boolean holiday,
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

        return records.stream().map(record -> {
            var enrollment = enrollmentByStudent.get(record.studentId());
            if (enrollment == null) {
                throw new NotFoundException("Student is not enrolled in this class session");
            }

            validateAttendanceState(record.present(), record.excused(), record.reason(), record.late());

            var existingOpt = attendanceRepo.findByEnrollmentAndDateAndSchoolId(enrollment.getId(), date, schoolId);

            Attendance attendance;
            if (existingOpt.isPresent()) {
                attendance = existingOpt.get();

                String prevStatus = attendance.getStatus();

                attendance.update(record.present(), record.excused(), record.late(), record.reason(), holiday);
                attendanceRepo.save(attendance);

                notifyParentForAttendanceChange(attendance, enrollment.getStudent().getParent(), prevStatus);
            } else {
                var id = UUID.randomUUID().toString();
                attendance = Attendance.create(id, schoolId, enrollment, date, record.present(), record.excused(),
                        record.late(), record.reason(), holiday);
                attendanceRepo.save(attendance);

                notifyParentForAttendanceChange(attendance, enrollment.getStudent().getParent(), null);
            }

            return AttendanceMapper.toDTO(attendance);
        }).toList();
    }

    private void notifyParentForAttendanceChange(Attendance attendance, Parent parent, String prevStatus) {
        if (parent == null)
            return;

        Map<String, String> meta = Map.of(
                "student_id", attendance.getEnrollment().getStudent().getId(),
                "student_name", attendance.getEnrollment().getStudent().getName(),
                "attendance_date", attendance.getDate().toString(),
                "status", attendance.getStatus(),
                "prev_status", prevStatus != null ? prevStatus : "",
                "corrected", prevStatus != null ? "true" : "false");

        String title = "Attendance Update for " + attendance.getEnrollment().getStudent().getName() + " on "
                + attendance.getDate();
        String message;

        if (attendance.isLate()) {
            message = "Your child was marked Late for " + attendance.getDate();
        } else if (!attendance.isPresent() && !attendance.isExcused()) {
            message = "Your child was marked Absent for " + attendance.getDate();
        } else if (attendance.isPresent() && prevStatus == "Absent") {
            message = "Correction: Your child is now marked Present for " + attendance.getDate();
        } else {
            return;
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
