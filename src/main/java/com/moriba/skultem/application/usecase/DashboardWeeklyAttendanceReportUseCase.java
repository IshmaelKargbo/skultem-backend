package com.moriba.skultem.application.usecase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.WeeklyAttendanceDTO;
import com.moriba.skultem.domain.repository.AttendanceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DashboardWeeklyAttendanceReportUseCase {

    private final AttendanceRepository attendanceRepo;

    public List<WeeklyAttendanceDTO> weeklyAttendance(String schoolId) {

        LocalDate today = LocalDate.now();

        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.FRIDAY);

        var rows = attendanceRepo.weeklyAttendance(
                schoolId,
                startOfWeek,
                endOfWeek);

        return rows.stream().map(r -> {

            String day = (String) r[0];
            long present = ((Number) r[1]).longValue();
            long total = ((Number) r[2]).longValue();

            int rate = 0;

            if (total > 0) {
                rate = (int) ((present * 100) / total);
            }

            return new WeeklyAttendanceDTO(day.trim(), rate);
        }).toList();
    }
}
