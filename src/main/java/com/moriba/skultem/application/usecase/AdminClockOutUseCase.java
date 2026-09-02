package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClockOutResponseDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Mirrors AdminClockInUseCase - an admin clocking a teacher out on their behalf, no geofence/IP
// checks. A teacher must already have a clock-in row for today (self or admin-clocked) before an
// admin can clock them out.
@Service
@Transactional
@RequiredArgsConstructor
public class AdminClockOutUseCase {
    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final HttpServletRequest request;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_CLOCKED_OUT_BY_ADMIN")
    public ClockOutResponseDTO execute(String schoolId, String teacherId) {
        var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        String ip = getClientIp();
        var today = LocalDate.now();
        var attendance = attendanceRepo.findByTeacherIdAndSchoolIdAndDate(teacher.getId(), schoolId, today)
                .orElseThrow(() -> new BadRequestException("This teacher hasn't been clocked in today yet"));

        if (attendance.alreadyClockedOut()) {
            return new ClockOutResponseDTO(true, attendance.getClockedOutAt(), 0);
        }

        attendance.applyAdminClockOut(ip);
        attendanceRepo.save(attendance);

        logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher clocked out by admin",
                teacher.getUser().getName(), null, attendance.getId());

        return new ClockOutResponseDTO(false, attendance.getClockedOutAt(), 0);
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
