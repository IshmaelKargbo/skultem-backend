package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.DashboardDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
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
        private final AcademicYearRepository academicYearRepo;

        public DashboardDTO getDashboardSummary(String schoolId) {
                AcademicYear academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));

                StudentCalucation studentCount = calucationStudentCount(schoolId, academicYear.getId());
                long totalTeachers = teacherRepo.countAllBySchool(schoolId);

                String activeYearStr = academicYear.getName();

                Term term = termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId())
                                .orElseThrow(() -> new NotFoundException("Active term not found"));
                String activeTermStr = term.getName();

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
