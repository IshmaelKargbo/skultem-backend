package com.moriba.skultem.application.services;

import java.time.LocalDate;
import java.util.Comparator;
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
import com.moriba.skultem.application.usecase.ClockInUseCase;
import com.moriba.skultem.application.usecase.ClockOutUseCase;
import com.moriba.skultem.application.usecase.MarkTeacherAttendanceUseCase;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.TeacherAttendance;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.infrastructure.rest.dto.TeacherAttendanceRecordDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherAttendanceService {

    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final MarkTeacherAttendanceUseCase markTeacherAttendanceUseCase;
    private final ClockInUseCase clockInUseCase;
    private final ClockOutUseCase clockOutUseCase;
    private final AdminClockInUseCase adminClockInUseCase;
    private final AdminClockOutUseCase adminClockOutUseCase;

    public ClockInResponseDTO clockIn(String schoolId, String userId, double latitude, double longitude) {
        return clockInUseCase.execute(schoolId, userId, latitude, longitude);
    }

    public ClockOutResponseDTO clockOut(String schoolId, String userId, double latitude, double longitude) {
        return clockOutUseCase.execute(schoolId, userId, latitude, longitude);
    }

    public void mark(String schoolId, LocalDate date, List<TeacherAttendanceRecordDTO> records) {
        markTeacherAttendanceUseCase.execute(schoolId, date, records);
    }

    public MyAttendanceTodayDTO myTodayStatus(String schoolId, String userId) {
        var teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Teacher profile not found for this account"));

        var existing = attendanceRepo.findByTeacherIdAndSchoolIdAndDate(teacher.getId(), schoolId, LocalDate.now());

        return existing.map(a -> new MyAttendanceTodayDTO(a.getStatus(), a.getClockedInAt(), a.getClockedOutAt()))
                .orElse(new MyAttendanceTodayDTO(null, null, null));
    }

    public TeacherAttendanceRosterDTO roster(String schoolId, LocalDate date) {
        // search() has no status filter - it also returns INACTIVE/DELETED teachers, who have no
        // business showing up on a register to mark.
        List<Teacher> teachers = teacherRepo.search("", schoolId, Pageable.unpaged()).getContent().stream()
                .filter(t -> t.getStatus() == Teacher.Status.ACTIVE)
                .toList();

        Map<String, TeacherAttendance> marked = attendanceRepo.findAllBySchoolIdAndDate(schoolId, date).stream()
                .collect(Collectors.toMap(a -> a.getTeacher().getId(), a -> a));

        List<TeacherRosterEntryDTO> entries = teachers.stream()
                .map(teacher -> {
                    var attendance = marked.get(teacher.getId());
                    TeacherDTO teacherDTO = TeacherMapper.toDTO(teacher);

                    return attendance != null
                            ? new TeacherRosterEntryDTO(teacherDTO, attendance.getStatus(), attendance.getNote(),
                                    attendance.getClockedInAt(), attendance.getClockInIp(),
                                    attendance.getClockedOutAt(), attendance.getClockOutIp(),
                                    attendance.isClockInByAdmin(), attendance.isClockOutByAdmin())
                            : new TeacherRosterEntryDTO(teacherDTO, null, null, null, null, null, null, false, false);
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

    // Admin clocking a teacher in/out on their behalf - no geofence/IP checks (that's the whole
    // point: it's for staff an admin can't rely on to self-service, e.g. an internet outage on
    // their end). See AdminClockInUseCase/AdminClockOutUseCase.
    public ClockInResponseDTO adminClockIn(String schoolId, String teacherId) {
        return adminClockInUseCase.execute(schoolId, teacherId);
    }

    public ClockOutResponseDTO adminClockOut(String schoolId, String teacherId) {
        return adminClockOutUseCase.execute(schoolId, teacherId);
    }

    public TeacherAttendanceHistoryPageDTO history(String schoolId, int page, int size) {
        var byDate = attendanceRepo.findAllBySchoolId(schoolId).stream()
                .collect(Collectors.groupingBy(TeacherAttendance::getDate));

        var summaries = byDate.entrySet().stream()
                .map(entry -> toDaySummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TeacherAttendanceDaySummaryDTO::date).reversed())
                .toList();

        if (size <= 0) {
            return new TeacherAttendanceHistoryPageDTO(summaries, summaries.size());
        }

        int from = Math.min((page - 1) * size, summaries.size());
        int to = Math.min(from + size, summaries.size());

        return new TeacherAttendanceHistoryPageDTO(summaries.subList(from, to), summaries.size());
    }

    private TeacherAttendanceDaySummaryDTO toDaySummary(LocalDate date, List<TeacherAttendance> records) {
        long present = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.PRESENT).count();
        long late = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.LATE).count();
        long absent = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.ABSENT).count();
        long excused = records.stream().filter(a -> a.getStatus() == TeacherAttendance.Status.EXCUSED).count();
        long total = records.size();
        double rate = total == 0 ? 0 : Math.round((present + late) * 1000.0 / total) / 10.0;

        return new TeacherAttendanceDaySummaryDTO(date, present, late, absent, excused, total, rate);
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
