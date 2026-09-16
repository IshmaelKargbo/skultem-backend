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
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;
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

        public ClassSessionAttendanceDTO execute(String schoolId, String classSessionId, LocalDate date) {
                var classSession = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Class session not found"));

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
                                                null);
                        }

                        var mark = attendance.get();
                        String recordedBy = resolveRecorderName(mark.getRecordedByUserId(), recorderNameCache);
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
                                        mark.getUpdatedAt());
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
                                records);
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
