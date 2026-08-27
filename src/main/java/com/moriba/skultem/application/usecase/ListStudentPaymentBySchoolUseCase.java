package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.mapper.PaymentMapper;
import com.moriba.skultem.domain.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentPaymentBySchoolUseCase {
    private final PaymentRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public Page<PaymentDTO> execute(String schoolId, String academicYearId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        return repo.findAllByAcademicYearAndSchoolId(academicYear.getId(), schoolId, pageable).map(PaymentMapper::toDTO);
    }
}
