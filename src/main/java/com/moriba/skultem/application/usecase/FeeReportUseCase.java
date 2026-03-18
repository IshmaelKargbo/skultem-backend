package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.mapper.StudentFeeMapper;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FeeReportUseCase {

    private final StudentFeeRepository repo;
    private final PaymentRepository paymentRepo;

    public Page<StudentFeeDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0)
                ? PageRequest.of(page - 1, size)
                : Pageable.unpaged();

        List<Filter> filters = request.filters();

        List<Filter> dbFilters = filters.stream()
                .filter(f -> !"status".equalsIgnoreCase(f.field()))
                .collect(Collectors.toList());

        Filter statusFilter = filters.stream()
                .filter(f -> "status".equalsIgnoreCase(f.field()))
                .findFirst()
                .orElse(null);

        Page<StudentFee> res = repo.runReport(
                request.schoolId(),
                dbFilters,
                pageable);

        List<StudentFeeDTO> mapped = res.stream()
                .map(e -> {

                    BigDecimal amountPaid = paymentRepo.sumPaymentsByStudentAndFee(
                            e.getEnrollment().getStudent().getId(),
                            e.getFee().getId());

                    if (amountPaid == null) {
                        amountPaid = BigDecimal.ZERO;
                    }

                    StudentFeeDTO dto = StudentFeeMapper.toDTO(e, amountPaid);
                    
                    return dto;
                })
                .collect(Collectors.toList());

        if (statusFilter != null && statusFilter.value() != null) {

            String status = statusFilter.value().toString();

            mapped = mapped.stream()
                    .filter(dto -> dto.status().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

  
        return new PageImpl<>(
                mapped,
                pageable,
                mapped.size());
    }
}