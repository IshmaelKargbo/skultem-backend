package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClockOutResponseDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Mirrors ClockInUseCase - same geofence + optional IP checks, applied to a teacher's own clock-in
// row for today. A teacher can't clock out before they've clocked in (nothing to close out).
@Service
@Transactional
@RequiredArgsConstructor
public class ClockOutUseCase {
    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final AttendanceLocationSettingRepository locationRepo;
    private final HttpServletRequest request;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_CLOCKED_OUT")
    public ClockOutResponseDTO execute(String schoolId, String userId, double latitude, double longitude) {
        // Scoped by school - an unscoped findByUserId throws NonUniqueResultException for a
        // teacher who works at more than one school. See CurriculumService for the same fix.
        var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new NotFoundException(
                        "You haven't been added to staff/payroll records yet - ask your admin to include you from your profile"));

        var settings = locationRepo.findBySchoolId(schoolId)
                .orElseThrow(() -> new BadRequestException(
                        "Clock-in location has not been set up yet - contact your school admin"));

        String ip = getClientIp();

        if (settings.hasIpRestriction() && !settings.isIpAllowed(ip)) {
            throw new BadRequestException("Clock-out must be done from the school's network.");
        }

        double distance = settings.distanceMetersTo(latitude, longitude);

        if (!settings.isWithinRange(latitude, longitude)) {
            throw new BadRequestException(String.format(
                    "You're about %.0fm from the school - you need to be within %dm to clock out.",
                    distance, settings.getRadiusMeters()));
        }

        var today = LocalDate.now();
        var attendance = attendanceRepo.findByTeacherIdAndSchoolIdAndDate(teacher.getId(), schoolId, today)
                .orElseThrow(() -> new BadRequestException("You haven't clocked in today yet"));

        if (attendance.alreadyClockedOut()) {
            return new ClockOutResponseDTO(true, attendance.getClockedOutAt(), distance);
        }

        attendance.applyClockOut(ip);
        attendanceRepo.save(attendance);

        logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher clocked out", teacher.getUser().getName(),
                null, attendance.getId());

        return new ClockOutResponseDTO(false, attendance.getClockedOutAt(), distance);
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
