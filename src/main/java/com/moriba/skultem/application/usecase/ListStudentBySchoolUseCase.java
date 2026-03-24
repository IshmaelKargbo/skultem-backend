package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.Metric;
import com.moriba.skultem.infrastructure.widget.WidgetService;
import com.moriba.skultem.infrastructure.widget.dto.Widget;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentBySchoolUseCase {
    private final StudentRepository repo;
    private final EnrollmentRepository enrollmentRepo;
    private final AcademicYearRepository academicYearRepo;
    private WidgetService widgetService;

    public Page<StudentDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
        return repo.findBySchoolId(schoolId, pageable).map(e -> {
            Enrollment enrollment = enrollmentRepo
                    .findByStudentAndAcademicYearAndSchoolId(e.getId(), academicYear.getId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("enrollment not found"));
            return StudentMapper.toDTO(e, enrollment);
        });
    }

    public FeeDetail getFeeDetail(String schoolId) {

        var metrics = new ArrayList<Metric>();
        var filters = new ArrayList<Filter>();

        metrics.add(new Metric("Paid", "sum", "amountPaid", Map.of("field", "status", "value", "Paid"), null, null));
        metrics.add(new Metric("Pending", "sum", "amountPaid", Map.of("field", "status", "value", "Paid"), null, null));
        metrics.add(new Metric("Total", "sum", "amount", null, null, null));

        var request = new Widget("fees", filters, metrics, "bar", "Fees Details");
        var res = widgetService.runAnalytics(schoolId, null, 0, 0);

        return null;
    }

    public record FeeDetail(BigDecimal total, BigDecimal paid, BigDecimal balance, String status) {
    }
}
