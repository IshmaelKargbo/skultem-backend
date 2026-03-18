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

        private static final Set<String> COMPUTED_FIELDS = Set.of("state", "status");

        private final AttendanceRepository repo;
        private final AcademicYearRepository academicYearRepo;
        private final ClassSessionRepository classSessionRepo;

        public Page<AttendanceHistoryDTO> execute(String schoolId, String classId, int page, int size) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));
                var clazz = classSessionRepo
                                .findByIdAndSchoolId(classId, schoolId)
                                .orElseThrow(() -> new RuleException("class session not found"));

                Pageable pageable = Pageable.unpaged();
                if (size > 0) {
                        pageable = PageRequest.of(page, size);
                }

                return repo.fetchDailyClassAttendanceSummary(
                                clazz.getClazz().getId(), academicYear.getId(), schoolId, pageable);
        }

        public Page<AttendanceDTO> execute(ReportBuilderDTO request, int page, int size) {
                Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

                List<Filter> safeFilters = request.filters() == null
                                ? List.of()
                                : request.filters().stream()
                                                .filter(f -> f != null
                                                        && f.field() != null
                                                        && f.operator() != null
                                                        && !COMPUTED_FIELDS.contains(f.field().toLowerCase()))
                                                .toList();

                Page<Attendance> res = repo.runReport(request.schoolId(), safeFilters, pageable);
                return res.map(AttendanceMapper::toDTO);
        }

        public AttendanceRateDTO computeRate(String schoolId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));

                List<AttendanceHistoryDTO> summaries = repo
                                .fetchDailyClassAttendanceSummary(null, academicYear.getId(), schoolId,
                                                Pageable.unpaged())
                                .getContent();

                long totalPresent = summaries.stream()
                                .mapToLong(s -> s.presentCount() != null ? s.presentCount() : 0L)
                                .sum();

                long totalCount = summaries.stream()
                                .mapToLong(s -> s.totalCount() != null ? s.totalCount() : 0L)
                                .sum();

                double rate = totalCount > 0 ? (totalPresent * 100.0 / totalCount) : 0.0;

                return new AttendanceRateDTO(totalPresent, totalCount, Math.round(rate * 10.0) / 10.0);
        }

        public record AttendanceRateDTO(long presentCount, long totalCount, double rate) {}
}