package com.moriba.skultem.application.usecase;

import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherAttendanceSummaryRowDTO;
import com.moriba.skultem.application.mapper.TeacherAttendanceSummaryRowMapper;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GenerateMonthlyTeacherAttendanceSummaryUseCase {

    private final TeacherRepository teacherRepo;
    private final TeacherAttendanceRepository attendanceRepo;

    public List<TeacherAttendanceSummaryRowDTO> execute(String schoolId, YearMonth month) {
        // search() has no status filter - it also returns INACTIVE/DELETED teachers, who have no
        // business appearing on a management report, same convention as
        // TeacherAttendanceService#roster.
        List<Teacher> activeTeachers = teacherRepo.search("", schoolId, Pageable.unpaged()).getContent().stream()
                .filter(t -> t.getStatus() == Teacher.Status.ACTIVE)
                .toList();

        var rows = attendanceRepo.attendanceCountsByTeacherAndDateRange(schoolId, month.atDay(1),
                month.atEndOfMonth());

        return TeacherAttendanceSummaryRowMapper.merge(activeTeachers, rows);
    }
}
