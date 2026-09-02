package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClockInResponseDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.TeacherAttendance;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// An admin clocking a teacher in on their behalf - for staff who can't reliably self-service
// through the geofenced Clock In (an internet or GPS issue on their end, or no portal access at
// all). Deliberately skips the location/IP checks in ClockInUseCase: an admin acting on someone
// else's behalf isn't the thing being verified here, their authority to do so already is (this
// endpoint sits behind the same ADMIN/OWNER/PROPRIETOR gate as the rest of the roster).
@Service
@Transactional
@RequiredArgsConstructor
public class AdminClockInUseCase {
    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final HttpServletRequest request;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_CLOCKED_IN_BY_ADMIN")
    public ClockInResponseDTO execute(String schoolId, String teacherId) {
        var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        String ip = getClientIp();
        var today = LocalDate.now();
        var existing = attendanceRepo.findByTeacherIdAndSchoolIdAndDate(teacher.getId(), schoolId, today);

        if (existing.isPresent()) {
            var attendance = existing.get();

            if (attendance.alreadyClockedIn()) {
                return new ClockInResponseDTO(true, attendance.getClockedInAt(), 0);
            }

            attendance.applyAdminClockIn(ip);
            attendanceRepo.save(attendance);

            logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher clocked in by admin",
                    teacher.getUser().getName(), null, attendance.getId());

            return new ClockInResponseDTO(false, attendance.getClockedInAt(), 0);
        }

        var attendance = TeacherAttendance.adminClockIn(UUID.randomUUID().toString(), schoolId, teacher, today, ip);
        attendanceRepo.save(attendance);

        logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher clocked in by admin",
                teacher.getUser().getName(), null, attendance.getId());

        return new ClockInResponseDTO(false, attendance.getClockedInAt(), 0);
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
