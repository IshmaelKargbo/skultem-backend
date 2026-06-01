package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.mapper.PaymentMapper;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentReportUseCase {

    private final PaymentRepository repo;

    public Page<PaymentDTO> execute(
            ReportBuilderDTO request,
            int page,
            int size) {

        Pageable pageable = createPageable(page, size);

        List<Filter> filters = request.filters() == null
                ? List.of()
                : request.filters();

        Page<Payment> result = repo.runReport(
                request.schoolId(),
                filters,
                pageable);

        return result.map(PaymentMapper::toDTO);
    }

    private Pageable createPageable(int page, int size) {

        if (size <= 0) {
            return Pageable.unpaged();
        }

        int pageNumber = Math.max(page - 1, 0);

        return PageRequest.of(pageNumber, size);
    }
}