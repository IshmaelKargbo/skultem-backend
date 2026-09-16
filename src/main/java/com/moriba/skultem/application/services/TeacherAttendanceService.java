package com.moriba.skultem.application.services;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClockInResponseDTO;
import com.moriba.skultem.application.dto.ClockOutResponseDTO;
import com.moriba.skultem.application.dto.MyAttendanceTodayDTO;
import com.moriba.skultem.application.dto.TeacherAttendanceDayDTO;
import com.moriba.skultem.application.dto.TeacherAttendanceDaySummaryDTO;
import com.moriba.skultem.application.dto.TeacherAttendanceHistoryPageDTO;
import com.moriba.skultem.application.dto.TeacherAttendanceRosterDTO;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.dto.TeacherRosterEntryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.application.usecase.AdminClockInUseCase;
import com.moriba.skultem.application.usecase.AdminClockOutUseCase;
import com.moriba.skultem.application.usecase.AdminUnclockUseCase;
import com.moriba.skultem.application.usecase.ClockInUseCase;
import com.moriba.skultem.application.usecase.ClockOutUseCase;
import com.moriba.skultem.application.usecase.MarkTeacherAttendanceUseCase;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.TeacherAttendance;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.infrastructure.rest.dto.TeacherAttendanceRecordDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherAttendanceService {

    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final UserRepository userRepo;
    private final MarkTeacherAttendanceUseCase markTeacherAttendanceUseCase;
    private final ClockInUseCase clockInUseCase;
    private final ClockOutUseCase clockOutUseCase;
    private final AdminClockInUseCase adminClockInUseCase;
    private final AdminClockOutUseCase adminClockOutUseCase;
    private final AdminUnclockUseCase adminUnclockUseCase;

    public ClockInResponseDTO clockIn(String schoolId, String userId, double latitude, double longitude, Double accuracyMeters) {
        return clockInUseCase.execute(schoolId, userId, latitude, longitude, accuracyMeters);
    }

    public ClockOutResponseDTO clockOut(String schoolId, String userId, double latitude, double longitude, Double accuracyMeters) {
        return clockOutUseCase.execute(schoolId, userId, latitude, longitude, accuracyMeters);
    }

    public void mark(String schoolId, LocalDate date, List<TeacherAttendanceRecordDTO> records) {
        markTeacherAttendanceUseCase.execute(schoolId, date, records);
    }

    public MyAttendanceTodayDTO myTodayStatus(String schoolId, String userId) {
        // Scoped by school - an unscoped findByUserId throws NonUniqueResultException for a
        // teacher who works at more than one school.
        var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new NotFoundException(
                        "You haven't been added to staff/payroll records yet - ask your admin to include you from your profile"));

        var existing = attendanceRepo.findByTeacherIdAndSchoolIdAndDate(teacher.getId(), schoolId,
                LocalDate.now(SchoolTimeZone.ZONE));

        return existing.map(a -> new MyAttendanceTodayDTO(a.getStatus(), a.getClockedInAt(), a.getClockedOutAt()))
                .orElse(new MyAttendanceTodayDTO(null, null, null));
    }

    public TeacherAttendanceRosterDTO roster(String schoolId, LocalDate date) {
        List<Teacher> teachers = activeTeachers(schoolId);

        Map<String, TeacherAttendance> marked = attendanceRepo.findAllBySchoolIdAndDate(schoolId, date).stream()
                .collect(Collectors.toMap(a -> a.getTeacher().getId(), a -> a));

        // Recorders repeat heavily within one day (usually the same one or two admins) - cache
        // resolved names for this call only, never across requests.
        Map<String, String> recorderNameCache = new HashMap<>();

        List<TeacherRosterEntryDTO> entries = teachers.stream()
                .map(teacher -> {
                    var attendance = marked.get(teacher.getId());
                    TeacherDTO teacherDTO = TeacherMapper.toDTO(teacher);

                    if (attendance == null) {
                        return new TeacherRosterEntryDTO(teacherDTO, null, null, null, null, null, null, false, false,
                                null);
                    }

                    String recordedBy = resolveRecorderName(attendance.getRecordedByUserId(), recorderNameCache);
                    return new TeacherRosterEntryDTO(teacherDTO, attendance.getStatus(), attendance.getNote(),
                            attendance.getClockedInAt(), attendance.getClockInIp(),
                            attendance.getClockedOutAt(), attendance.getClockOutIp(),
                            attendance.isClockInByAdmin(), attendance.isClockOutByAdmin(), recordedBy);
                })
                .toList();

        return buildRoster(date, entries);
    }

    // One teacher's own attendance within a range - powers the calendar on their profile page.
    public List<TeacherAttendanceDayDTO> forTeacher(String schoolId, String teacherId, LocalDate from, LocalDate to) {
        return attendanceRepo.findAllBySchoolIdAndTeacherIdBetween(schoolId, teacherId, from, to).stream()
                .map(a -> new TeacherAttendanceDayDTO(a.getDate(), a.getStatus(), a.getNote(), a.getClockedInAt(),
                        a.getClockedOutAt(), a.isClockInByAdmin(), a.isClockOutByAdmin()))
                .toList();
    }

    // Same as forTeacher, but self-service - resolves the teacher from the signed-in user rather
    // than an admin-supplied teacherId, so a teacher can see their own clock-in/out history from
    // their own portal (forTeacher above is admin/owner/proprietor-only, for viewing someone
    // else's profile).
    public List<TeacherAttendanceDayDTO> myHistory(String schoolId, String userId, LocalDate from, LocalDate to) {
        var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new NotFoundException(
                        "You haven't been added to staff/payroll records yet - ask your admin to include you from your profile"));

        return forTeacher(schoolId, teacher.getId(), from, to);
    }

    // Admin clocking a teacher in/out on their behalf - no geofence/IP checks (that's the whole
    // point: it's for staff an admin can't rely on to self-service, e.g. an internet outage on
    // their end). See AdminClockInUseCase/AdminClockOutUseCase.
    public ClockInResponseDTO adminClockIn(String schoolId, String teacherId) {
        return adminClockInUseCase.execute(schoolId, teacherId);
    }

    public ClockOutResponseDTO adminClockOut(String schoolId, String teacherId) {
        return adminClockOutUseCase.execute(schoolId, teacherId);
    }

    public void adminUnclock(String schoolId, String teacherId) {
        adminUnclockUseCase.execute(schoolId, teacherId);
    }

    public TeacherAttendanceHistoryPageDTO history(String schoolId, int page, int size) {
        var byDate = attendanceRepo.findAllBySchoolId(schoolId).stream()
                .collect(Collectors.groupingBy(TeacherAttendance::getDate));

        // The denominator is today's active-staff headcount, not "however many rows happen to
        // exist for that day" - a day where only 1 of 50 teachers was ever clocked/marked should
        // read "1/50", not "1/1". There's no historical headcount tracking, so the current active
        // count is the best available denominator for past days too, same as roster() already
        // does for any single date regardless of how long ago it was.
        long activeCount = activeTeachers(schoolId).size();

        var summaries = byDate.entrySet().stream()
                .map(entry -> toDaySummary(entry.getKey(), entry.getValue(), activeCount))
                .sorted(Comparator.comparing(TeacherAttendanceDaySummaryDTO::date).reversed())
                .toList();

        if (size <= 0) {
            return new TeacherAttendanceHistoryPageDTO(summaries, summaries.size());
        }

        int from = Math.min((page - 1) * size, summaries.size());
        int to = Math.min(from + size, summaries.size());

        return new TeacherAttendanceHistoryPageDTO(summaries.subList(from, to), summaries.size());
    }

    // search() has no status filter - it also returns INACTIVE/DELETED teachers, who have no
    // business showing up on a register to mark or counting toward a headcount.
    private List<Teacher> activeTeachers(String schoolId) {
        return teacherRepo.search("", schoolId, Pageable.unpaged()).getContent().stream()
                .filter(t -> t.getStatus() == Teacher.Status.ACTIVE)
                .toList();
    }

    private String resolveRecorderName(String recordedByUserId, Map<String, String> cache) {
        if (recordedByUserId == null) {
            return null;
        }

        return cache.computeIfAbsent(recordedByUserId,
                id -> userRepo.findById(id).map(u -> u.getName()).orElse(null));
    }

    private TeacherAttendanceDaySummaryDTO toDaySummary(LocalDate date, List<TeacherAttendance> records,
            long totalStaff) {
        long present = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.PRESENT).count();
        long late = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.LATE).count();
        long absent = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.ABSENT).count();
        long excused = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.EXCUSED).count();
        double rate = totalStaff == 0 ? 0 : Math.round((present + late) * 1000.0 / totalStaff) / 10.0;

        return new TeacherAttendanceDaySummaryDTO(date, present, late, absent, excused, totalStaff, rate);
    }

    private TeacherAttendanceRosterDTO buildRoster(LocalDate date, List<TeacherRosterEntryDTO> entries) {
        long present = entries.stream().filter(e -> e.status() == TeacherAttendance.Status.PRESENT).count();
        long late = entries.stream().filter(e -> e.status() == TeacherAttendance.Status.LATE).count();
        long absent = entries.stream().filter(e -> e.status() == TeacherAttendance.Status.ABSENT).count();
        long excused = entries.stream().filter(e -> e.status() == TeacherAttendance.Status.EXCUSED).count();
        long unmarked = entries.stream().filter(e -> e.status() == null).count();
        long total = entries.size();
        double rate = total == 0 ? 0 : Math.round((present + late) * 1000.0 / total) / 10.0;

        return new TeacherAttendanceRosterDTO(date, entries, present, late, absent, excused, unmarked, total, rate);
    }
}
