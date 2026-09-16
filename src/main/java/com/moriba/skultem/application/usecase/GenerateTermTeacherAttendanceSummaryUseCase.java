package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherAttendanceSummaryRowDTO;
import com.moriba.skultem.application.dto.TermTeacherAttendanceSummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherAttendanceSummaryRowMapper;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GenerateTermTeacherAttendanceSummaryUseCase {

    private final TeacherRepository teacherRepo;
    private final TermRepository termRepo;
    private final TeacherAttendanceRepository attendanceRepo;

    public TermTeacherAttendanceSummaryDTO execute(String schoolId, String termId) {
        var term = termRepo.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        List<Teacher> activeTeachers = teacherRepo.search("", schoolId, Pageable.unpaged()).getContent().stream()
                .filter(t -> t.getStatus() == Teacher.Status.ACTIVE)
                .toList();

        var rows = attendanceRepo.attendanceCountsByTeacherAndDateRange(schoolId, term.getStartDate(),
                term.getEndDate());

        var teachers = TeacherAttendanceSummaryRowMapper.merge(activeTeachers, rows);

        long totalPresent = teachers.stream().mapToLong(TeacherAttendanceSummaryRowDTO::present).sum();
        long totalAbsent = teachers.stream().mapToLong(TeacherAttendanceSummaryRowDTO::absent).sum();
        long totalLate = teachers.stream().mapToLong(TeacherAttendanceSummaryRowDTO::late).sum();
        long totalRecorded = totalPresent + totalAbsent + totalLate;

        double averageAttendance = AttendanceRateCalculator.rate(totalPresent + totalLate, totalRecorded);

        return new TermTeacherAttendanceSummaryDTO(teachers, term.getName(), teachers.size(), averageAttendance,
                totalPresent, totalAbsent, totalLate);
    }
}
