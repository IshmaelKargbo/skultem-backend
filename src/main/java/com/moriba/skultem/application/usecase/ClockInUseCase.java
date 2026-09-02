package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClockInResponseDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.TeacherAttendance;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The geofenced self-service clock-in: a teacher presses "Clock In" in the portal, their browser
// reports its GPS coordinates, and this only marks them PRESENT if that location falls within the
// school's configured radius. A second, optional layer checks the request's IP against an
// admin-configured allowlist - GPS alone can be spoofed by anyone slightly technical, but faking
// both a location AND appearing to be on the school's own network is a much higher bar. Neither
// check is skippable from the client - both are enforced here, server-side.
@Service
@Transactional
@RequiredArgsConstructor
public class ClockInUseCase {
    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final AttendanceLocationSettingRepository locationRepo;
    private final HttpServletRequest request;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_CLOCKED_IN")
    public ClockInResponseDTO execute(String schoolId, String userId, double latitude, double longitude) {
        var teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Teacher profile not found for this account"));

        var settings = locationRepo.findBySchoolId(schoolId)
                .orElseThrow(() -> new BadRequestException(
                        "Clock-in location has not been set up yet - contact your school admin"));

        String ip = getClientIp();

        if (settings.hasIpRestriction() && !settings.isIpAllowed(ip)) {
            throw new BadRequestException("Clock-in must be done from the school's network.");
        }

        double distance = settings.distanceMetersTo(latitude, longitude);

        if (!settings.isWithinRange(latitude, longitude)) {
            throw new BadRequestException(String.format(
                    "You're about %.0fm from the school - you need to be within %dm to clock in.",
                    distance, settings.getRadiusMeters()));
        }

        var today = LocalDate.now();
        var existing = attendanceRepo.findByTeacherIdAndSchoolIdAndDate(teacher.getId(), schoolId, today);

        if (existing.isPresent()) {
            var attendance = existing.get();

            if (attendance.alreadyClockedIn()) {
                return new ClockInResponseDTO(true, attendance.getClockedInAt(), distance);
            }

            attendance.applyClockIn(ip);
            attendanceRepo.save(attendance);

            logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher clocked in",
                    teacher.getUser().getName(), null, attendance.getId());

            return new ClockInResponseDTO(false, attendance.getClockedInAt(), distance);
        }

        var attendance = TeacherAttendance.clockIn(UUID.randomUUID().toString(), schoolId, teacher, today, ip);
        attendanceRepo.save(attendance);

        logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher clocked in", teacher.getUser().getName(),
                null, attendance.getId());

        return new ClockInResponseDTO(false, attendance.getClockedInAt(), distance);
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
