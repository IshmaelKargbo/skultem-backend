package com.moriba.skultem.application.usecase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AttendanceMapper;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.HolidayRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MarkClassSessionAttendanceUseCase {
    private final AttendanceRepository attendanceRepo;
    private final ClassSessionRepository classSessionRepo;
    private final AcademicYearRepository academicYearRepo;
    private final HolidayRepository holidayRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final ReferenceGeneratorUsecase rg;

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

            var existing = attendanceRepo.findByEnrollmentAndDateAndSchoolId(enrollment.getId(), date, schoolId);

            if (existing.isPresent()) {
                var attendance = existing.get();
                attendance.update(record.present(), record.excused(), record.late(), record.reason(), holiday);
                attendanceRepo.save(attendance);
                return AttendanceMapper.toDTO(attendance);
            }

            var id = rg.generate("ATTENDANCE", "ATD");
            var attendance = Attendance.create(id, schoolId, enrollment, date, record.present(), record.excused(),
                    record.late(), record.reason(), holiday);
            attendanceRepo.save(attendance);
            return AttendanceMapper.toDTO(attendance);
        }).toList();
    }

    private List<Enrollment> loadSessionEnrollments(ClassSession classSession, String schoolId) {
        return enrollmentRepo.findAllByClassAndAcademicSchoolId(
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
