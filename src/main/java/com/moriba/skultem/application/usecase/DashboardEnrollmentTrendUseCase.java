package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MonthlyEnrollmentDTO;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardEnrollmentTrendUseCase {

        private final EnrollmentRepository enrollmentRepo;
        private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

        public List<MonthlyEnrollmentDTO> monthlyEnrollmentTrend(String schoolId, String academicYearId) {
                AcademicYear academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                ZoneId zone = ZoneId.systemDefault();

                Instant start = academicYear.getStartDate().atStartOfDay(zone).toInstant();
                Instant end = academicYear.getEndDate().atTime(23, 59, 59).atZone(zone).toInstant();

                var enrollments = enrollmentRepo.findBySchoolIdAndAcademicYearAndCreatedAtBetween(
                                schoolId, academicYear.getId(), start, end);

                Map<Month, Long> monthlyCounts = new LinkedHashMap<>();
                ZonedDateTime startZdt = start.atZone(zone);
                ZonedDateTime endZdt = end.atZone(zone);

                Month startMonth = startZdt.getMonth();
                Month endMonth = endZdt.getMonth();
                Month m = startMonth;
                do {
                        monthlyCounts.put(m, 0L);
                        m = m.plus(1);
                } while (m != endMonth.plus(1));

                enrollments.stream()
                                .collect(Collectors.groupingBy(
                                                e -> e.getCreatedAt().atZone(zone).getMonth(),
                                                Collectors.counting()))
                                .forEach(monthlyCounts::put);

                return monthlyCounts.entrySet().stream()
                                .map(e -> new MonthlyEnrollmentDTO(e.getKey().name().substring(0, 3), e.getValue()))
                                .toList();
        }
}