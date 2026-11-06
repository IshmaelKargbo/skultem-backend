package com.moriba.skultem.application.usecase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSessionAttendanceDTO;
import com.moriba.skultem.application.dto.ClassSessionAttendanceRecordDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.AttendanceRulesResolver;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetClassSessionAttendanceUseCase {
        private final AttendanceRepository attendanceRepo;
        private final ClassSessionRepository classSessionRepo;
        private final HolidayRepository holidayRepo;
        private final EnrollmentRepository enrollmentRepo;
        private final UserRepository userRepo;
        private final SchoolRepository schoolRepo;
        private final AttendanceRulesResolver attendanceRulesResolver;

        public ClassSessionAttendanceDTO execute(String schoolId, String classSessionId, LocalDate date) {
                return execute(schoolId, classSessionId, date, null);
        }

        // With roll (1 or 2) on a two-roll-call class, each record shows that roll call's mark so the
        // teacher edits just that one; without it, records show the result for the whole day.
        public ClassSessionAttendanceDTO execute(String schoolId, String classSessionId, LocalDate date,
                        Integer roll) {
                var classSession = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Class session not found"));

                int rollCalls = schoolRepo.findById(schoolId)
                                .map(s -> attendanceRulesResolver.rollCallsForLevel(s,
                                                classSession.getClazz().getLevel()))
                                .orElse(1);
                Integer shownRoll = rollCalls == 2 && roll != null && (roll == 1 || roll == 2) ? roll : null;

                var enrollments = loadSessionEnrollments(classSession, schoolId);

                // Recorders repeat heavily within one session/date (usually the same one or two
                // teachers) - cache resolved names for this call only, never across requests.
                Map<String, String> recorderNameCache = new HashMap<>();

                var records = enrollments.stream().map(enrollment -> {
                        var student = enrollment.getStudent();
                        var attendance = attendanceRepo.findByEnrollmentAndDateAndSchoolId(enrollment.getId(), date,
                                        schoolId);

                        String gender = student.getGender() != null ? student.getGender().name() : null;

                        if (attendance.isEmpty()) {
                                return new ClassSessionAttendanceRecordDTO(
                                                null,
                                                enrollment.getId(),
                                                student.getId(),
                                                student.getAdmissionNumber(),
                                                student.getName(),
                                                gender,
                                                student.getPhoto(),
                                                false,
                                                false,
                                                false,
                                                false,
                                                false,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null);
                        }

                        var mark = attendance.get();
                        String recordedBy = resolveRecorderName(mark.getRecordedByUserId(), recorderNameCache);
                        String first = mark.getFirstRoll() != null ? mark.getFirstRoll().name() : null;
                        String second = mark.getSecondRoll() != null ? mark.getSecondRoll().name() : null;

                        if (shownRoll != null) {
                                // This roll call only. A day marked before the school switched to two roll
                                // calls has no roll marks, so it shows as not taken yet.
                                var rollMark = mark.rollMark(shownRoll);
                                return new ClassSessionAttendanceRecordDTO(
                                                rollMark != null ? mark.getId() : null,
                                                enrollment.getId(),
                                                student.getId(),
                                                student.getAdmissionNumber(),
                                                String.join(" ", student.getGivenNames(), student.getFamilyName()),
                                                gender,
                                                student.getPhoto(),
                                                rollMark != null,
                                                mark.isHoliday(),
                                                rollMark == Attendance.RollMark.PRESENT,
                                                rollMark == Attendance.RollMark.EXCUSED,
                                                rollMark == Attendance.RollMark.LATE,
                                                rollMark == Attendance.RollMark.EXCUSED ? mark.getReason() : null,
                                                recordedBy,
                                                mark.getUpdatedAt(),
                                                first,
                                                second);
                        }

                        return new ClassSessionAttendanceRecordDTO(
                                        mark.getId(),
                                        enrollment.getId(),
                                        student.getId(),
                                        student.getAdmissionNumber(),
                                        String.join(" ", student.getGivenNames(), student.getFamilyName()),
                                        gender,
                                        student.getPhoto(),
                                        true,
                                        mark.isHoliday(),
                                        mark.isPresent(),
                                        mark.isExcused(),
                                        mark.isLate(),
                                        mark.getReason(),
                                        recordedBy,
                                        mark.getUpdatedAt(),
                                        first,
                                        second);
                }).toList();

                int totalStudents = records.size();
                int markedCount = (int) records.stream().filter(a -> a.marked()).count();
                int unmarkedCount = totalStudents - markedCount;
                int presentCount = (int) records.stream().filter(a -> a.present()).count();
                int excusedCount = (int) records.stream().filter(a -> a.excused()).count();
                int lateCount = (int) records.stream().filter(a -> a.late()).count();
                int absentCount = markedCount - presentCount - lateCount - excusedCount;

                int totalBoys = (int) records.stream().filter(a -> "MALE".equals(a.gender())).count();
                int totalGirls = (int) records.stream().filter(a -> "FEMALE".equals(a.gender())).count();
                // "Present" here means attended (present or late), same convention used everywhere
                // else attendance is aggregated.
                int presentBoys = (int) records.stream()
                                .filter(a -> "MALE".equals(a.gender()) && (a.present() || a.late())).count();
                int presentGirls = (int) records.stream()
                                .filter(a -> "FEMALE".equals(a.gender()) && (a.present() || a.late())).count();

                List<LocalDate> schoolHolidays = holidayRepo
                                .findAllBySchoolIdAndAcademicYear(schoolId, classSession.getAcademicYear().getId(),
                                                Pageable.unpaged())
                                .getContent()
                                .stream()
                                .map(h -> h.getDate())
                                .toList();

                boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SATURDAY
                                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                                || schoolHolidays.contains(date);

                return new ClassSessionAttendanceDTO(
                                classSessionId,
                                date,
                                isHoliday,
                                totalStudents,
                                markedCount,
                                unmarkedCount,
                                presentCount,
                                absentCount,
                                excusedCount,
                                lateCount,
                                totalBoys,
                                totalGirls,
                                presentBoys,
                                presentGirls,
                                records,
                                rollCalls,
                                shownRoll);
        }

        private String resolveRecorderName(String recordedByUserId, Map<String, String> cache) {
                if (recordedByUserId == null) {
                        return null;
                }

                return cache.computeIfAbsent(recordedByUserId,
                                id -> userRepo.findById(id).map(u -> u.getName()).orElse(null));
        }

        private List<Enrollment> loadSessionEnrollments(ClassSession classSession, String schoolId) {
                return enrollmentRepo.findAllByClassAndAcademicAndSchoolId(
                                classSession.getClazz().getId(),
                                classSession.getAcademicYear().getId(),
                                schoolId, Pageable.unpaged()).stream().filter(enrollment -> {
                                        boolean sectionMatch = enrollment.getSection() != null
                                                        && classSession.getSection() != null
                                                        && enrollment.getSection().getId()
                                                                        .equals(classSession.getSection().getId());

                                        if (!sectionMatch) {
                                                return false;
                                        }

                                        if (classSession.getStream() == null) {
                                                return enrollment.getStream() == null;
                                        }

                                        return enrollment.getStream() != null
                                                        && enrollment.getStream().getId()
                                                                        .equals(classSession.getStream().getId());
                                }).toList();
        }
}
