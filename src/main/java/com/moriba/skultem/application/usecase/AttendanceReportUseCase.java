package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceDTO;
import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AttendanceMapper;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceReportUseCase {

    private static final Set<String> COMPUTED_FIELDS = Set.of(
            "state",
            "status");

    private final AttendanceRepository repo;
    private final AcademicYearRepository academicYearRepo;
    private final ClassSessionRepository classSessionRepo;

    /**
     * Daily attendance summary for a class.
     */
    public Page<AttendanceHistoryDTO> execute(
            String schoolId,
            String classId,
            int page,
            int size) {

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new RuleException("Active academic year not found"));

        var clazz = classSessionRepo
                .findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> new RuleException("Class session not found"));

        Pageable pageable = createPageable(page, size);

        return repo.fetchDailyClassAttendanceSummary(
                clazz.getClazz().getId(),
                academicYear.getId(),
                schoolId,
                pageable);
    }

    /**
     * Attendance report builder.
     */
    public Page<AttendanceDTO> execute(
            ReportBuilderDTO request,
            int page,
            int size) {

        Pageable pageable = createPageable(page, size);

        List<Filter> safeFilters = request.filters() == null
                ? List.of()
                : request.filters().stream()
                        .filter(f -> f != null
                                && f.field() != null
                                && f.operator() != null
                                && !COMPUTED_FIELDS.contains(f.field().toLowerCase()))
                        .toList();

        Page<Attendance> result = repo.runReport(
                request.schoolId(),
                safeFilters,
                pageable);

        return result.map(AttendanceMapper::toDTO);
    }

    /**
     * Overall attendance rate for active academic year.
     */
    public AttendanceRateDTO computeRate(String schoolId) {

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new RuleException("Active academic year not found"));

        List<AttendanceHistoryDTO> summaries = repo
                .fetchDailyClassAttendanceSummary(
                        null,
                        academicYear.getId(),
                        schoolId,
                        Pageable.unpaged())
                .getContent();

        long totalPresent = summaries.stream()
                .mapToLong(s -> s.presentCount() == null ? 0L : s.presentCount())
                .sum();

        long totalCount = summaries.stream()
                .mapToLong(s -> s.totalCount() == null ? 0L : s.totalCount())
                .sum();

        double rate = totalCount == 0
                ? 0.0
                : (totalPresent * 100.0) / totalCount;

        return new AttendanceRateDTO(
                totalPresent,
                totalCount,
                Math.round(rate * 10.0) / 10.0);
    }

    private Pageable createPageable(int page, int size) {

        if (size <= 0) {
            return Pageable.unpaged();
        }

        int pageNumber = Math.max(page - 1, 0);

        return PageRequest.of(pageNumber, size);
    }

    public record AttendanceRateDTO(
            long presentCount,
            long totalCount,
            double rate) {
    }
}