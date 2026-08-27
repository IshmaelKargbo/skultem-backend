package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.DashboardDTO;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DashboardReportUseCase {

        private final PaymentRepository paymentRepo;
        private final EnrollmentRepository enrollmentRepo;
        private final TeacherRepository teacherRepo;
        private final TermRepository termRepo;
        private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

        public DashboardDTO getDashboardSummary(String schoolId, String academicYearId) {
                AcademicYear academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                StudentCalucation studentCount = calucationStudentCount(schoolId, academicYear.getId());
                long totalTeachers = teacherRepo.countAllBySchool(schoolId);

                String activeYearStr = academicYear.getName();

                // A year being browsed rather than actually active has no ACTIVE term - fall back to
                // its last term instead of failing the whole dashboard.
                String activeTermStr = termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId())
                                .map(Term::getName)
                                .or(() -> termRepo.findByAcademicYearIdAndSchool(academicYear.getId(), schoolId).stream()
                                                .max(java.util.Comparator.comparingInt(Term::getTermNumber))
                                                .map(Term::getName))
                                .orElse("—");

                var revenue = calculatRevenue(schoolId);

                return new DashboardDTO(studentCount.total, studentCount.growth, totalTeachers, activeYearStr,
                                activeTermStr, revenue.monthly,
                                revenue.growth);
        }

        Revenue calculatRevenue(String schoolId) {
                ZoneId zone = ZoneId.systemDefault();

                Instant startOfMonth = LocalDate.now()
                                .withDayOfMonth(1)
                                .atStartOfDay(zone)
                                .toInstant();

                Instant endOfMonth = LocalDate.now()
                                .withDayOfMonth(LocalDate.now().lengthOfMonth())
                                .atTime(23, 59, 59)
                                .atZone(zone)
                                .toInstant();

                ZonedDateTime startZdt = startOfMonth.atZone(zone);

                Instant prevMonthStart = startZdt.minusMonths(1).withDayOfMonth(1).toInstant();
                Instant prevMonthEnd = startZdt.minusMonths(1)
                                .withDayOfMonth(startZdt.minusMonths(1).toLocalDate().lengthOfMonth())
                                .withHour(23).withMinute(59).withSecond(59)
                                .toInstant();

                BigDecimal monthlyRevenue = paymentRepo.sumPaymentsBySchoolAndDateRange(schoolId, startOfMonth,
                                endOfMonth);

                BigDecimal prevMonthRevenue = paymentRepo.sumPaymentsBySchoolAndDateRange(
                                schoolId, prevMonthStart, prevMonthEnd);

                BigDecimal revenueGrowthPercent = BigDecimal.ZERO;
                if (prevMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
                        revenueGrowthPercent = monthlyRevenue.subtract(prevMonthRevenue)
                                        .divide(prevMonthRevenue, 2, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                }

                return new Revenue(monthlyRevenue, revenueGrowthPercent);
        }

        StudentCalucation calucationStudentCount(String schoolId, String academicYearId) {
                long totalStudents = enrollmentRepo.countByAcademicSchoolId(academicYearId, schoolId);
                ZoneId zone = ZoneId.systemDefault();

                Instant lastMonth = LocalDate.now()
                                .minusMonths(1)
                                .withDayOfMonth(1)
                                .atStartOfDay(zone)
                                .toInstant();

                long lastMonthStudents = enrollmentRepo.countBySchoolIdAndAcademicYearAndCreatedBefore(
                                schoolId,
                                academicYearId,
                                lastMonth);

                long studentGrowth = totalStudents - lastMonthStudents;

                return new StudentCalucation(totalStudents, studentGrowth);
        }

        record Revenue(BigDecimal monthly, BigDecimal growth) {
        }

        record StudentCalucation(long total, long growth) {
        }
}
