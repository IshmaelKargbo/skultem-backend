package com.moriba.skultem.application.usecase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.WeeklyGenderAttendanceDTO;
import com.moriba.skultem.application.dto.WeeklyGenderAttendanceDayDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;
import com.moriba.skultem.domain.vo.Gender;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Weekly attendance by gender for one class, built on top of the existing attendance engine
// (AttendanceRepository/AttendanceRateCalculator) rather than a new one, per the requirement that
// staff never re-enter boys/girls totals by hand. Enrolled totals come from the class roster
// (current gender + active enrollment for the academic year), attendance counts come from the
// existing daily attendance records - the same two data sources every other attendance report in
// the app already uses.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateWeeklyGenderAttendanceReportUseCase {

    private final ClassRepository classRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AttendanceRepository attendanceRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public WeeklyGenderAttendanceDTO execute(String schoolId, String classId, String academicYearId,
            LocalDate weekOf) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        LocalDate start = weekOf.with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(4);

        var enrollments = enrollmentRepo
                .findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), schoolId, Pageable.unpaged())
                .getContent();

        int boysEnrolled = (int) enrollments.stream()
                .filter(e -> e.getStudent().getGender() == Gender.MALE)
                .count();
        int girlsEnrolled = (int) enrollments.stream()
                .filter(e -> e.getStudent().getGender() == Gender.FEMALE)
                .count();

        var rows = attendanceRepo.attendanceCountsByClassGenderAndDateRange(schoolId, classId, academicYear.getId(),
                start, end);

        Map<LocalDate, long[]> presentByDate = new LinkedHashMap<>(); // [boysPresent, girlsPresent]
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            Gender gender = (Gender) row[1];
            long present = ((Number) row[2]).longValue();

            long[] counts = presentByDate.computeIfAbsent(date, k -> new long[2]);
            if (gender == Gender.MALE) {
                counts[0] += present;
            } else {
                counts[1] += present;
            }
        }

        List<WeeklyGenderAttendanceDayDTO> days = new ArrayList<>();
        long totalBoysPresent = 0;
        long totalGirlsPresent = 0;
        long daysCount = 0;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            long[] counts = presentByDate.getOrDefault(date, new long[2]);
            long boysPresent = counts[0];
            long girlsPresent = counts[1];
            long boysAbsent = Math.max(0, boysEnrolled - boysPresent);
            long girlsAbsent = Math.max(0, girlsEnrolled - girlsPresent);

            double boysPct = AttendanceRateCalculator.rate(boysPresent, boysEnrolled);
            double girlsPct = AttendanceRateCalculator.rate(girlsPresent, girlsEnrolled);
            double overallPct = AttendanceRateCalculator.rate(boysPresent + girlsPresent,
                    (long) boysEnrolled + girlsEnrolled);

            days.add(new WeeklyGenderAttendanceDayDTO(date, date.getDayOfWeek().toString(),
                    (int) boysPresent, (int) boysAbsent, (int) girlsPresent, (int) girlsAbsent,
                    boysPct, girlsPct, overallPct));

            totalBoysPresent += boysPresent;
            totalGirlsPresent += girlsPresent;
            daysCount++;
        }

        double overallBoysPct = AttendanceRateCalculator.rate(totalBoysPresent, (long) boysEnrolled * daysCount);
        double overallGirlsPct = AttendanceRateCalculator.rate(totalGirlsPresent, (long) girlsEnrolled * daysCount);
        double overallPct = AttendanceRateCalculator.rate(totalBoysPresent + totalGirlsPresent,
                (long) (boysEnrolled + girlsEnrolled) * daysCount);

        return new WeeklyGenderAttendanceDTO(classId, clazz.getName(), start, end, boysEnrolled, girlsEnrolled,
                days, overallPct, overallBoysPct, overallGirlsPct);
    }
}
