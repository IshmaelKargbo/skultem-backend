package com.moriba.skultem.application.usecase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

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

    public ClassSessionAttendanceDTO execute(String schoolId, String classSessionId, LocalDate date) {
        var classSession = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var enrollments = loadSessionEnrollments(classSession, schoolId);

        var records = enrollments.stream().map(enrollment -> {
            var student = enrollment.getStudent();
            var attendance = attendanceRepo.findByEnrollmentAndDateAndSchoolId(enrollment.getId(), date, schoolId);

            if (attendance.isEmpty()) {
                return new ClassSessionAttendanceRecordDTO(
                        null,
                        enrollment.getId(),
                        student.getId(),
                        student.getAdmissionNumber(),
                        student.getName(),
                        false,
                        false,
                        false,
                        false,
                        false,
                        null);
            }

            var mark = attendance.get();
            return new ClassSessionAttendanceRecordDTO(
                    mark.getId(),
                    enrollment.getId(),
                    student.getId(),
                    student.getAdmissionNumber(),
                    String.join(" ", student.getGivenNames(), student.getFamilyName()),
                    true,
                    mark.isHoliday(),
                    mark.isPresent(),
                    mark.isExcused(),
                    mark.isLate(),
                    mark.getReason());
        }).toList();

        int totalStudents = records.size();
        int markedCount = (int) records.stream().filter(ClassSessionAttendanceRecordDTO::marked).count();
        int unmarkedCount = totalStudents - markedCount;
        int presentCount = (int) records.stream().filter(ClassSessionAttendanceRecordDTO::present).count();
        int excusedCount = (int) records.stream().filter(ClassSessionAttendanceRecordDTO::excused).count();
        int lateCount = (int) records.stream().filter(ClassSessionAttendanceRecordDTO::late).count();
        int absentCount = markedCount - presentCount - lateCount - excusedCount;

        List<LocalDate> schoolHolidays = holidayRepo
                .findAllBySchoolIdAndAcademicYear(schoolId, classSession.getAcademicYear().getId(), Pageable.unpaged())
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
                records);
    }

    private List<Enrollment> loadSessionEnrollments(ClassSession classSession, String schoolId) {
        return enrollmentRepo.findAllByClassAndAcademicSchoolId(
                classSession.getClazz().getId(),
                classSession.getAcademicYear().getId(),
                schoolId).stream().filter(enrollment -> {
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
}
