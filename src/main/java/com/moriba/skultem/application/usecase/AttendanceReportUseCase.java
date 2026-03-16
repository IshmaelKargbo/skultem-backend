package com.moriba.skultem.application.usecase;

import java.util.List;

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
                return repo.fetchDailyClassAttendanceSummary(clazz.getClazz().getId(), academicYear.getId(), schoolId,
                                pageable);
        }

        public Page<AttendanceDTO> execute(ReportBuilderDTO request, int page, int size) {

                Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

                List<Filter> filters = request.filters();

                Page<Attendance> res = repo.runReport(
                                request.schoolId(),
                                filters,
                                pageable);

                return res.map(e -> AttendanceMapper.toDTO(e));
        }
}
