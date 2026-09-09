package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.mapper.StudentFeeMapper;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSubjectFeesByStudentUseCase {

    private final StudentFeeRepository repo;
    private final PaymentRepository paymentRepo;

    public Page<StudentFeeDTO> execute(String school, String studentId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findAllBySchoolAndStudent(school, studentId, pageable).map(e -> {
            // Was hardcoded to ZERO here, so every fee looked fully unpaid ("Pending", full
            // balance owed) on the parent's Fee Schedule regardless of what had actually been
            // paid - this is the real, per-fee sum of that student's payments against it.
            BigDecimal amountPaid = Optional
                    .ofNullable(paymentRepo.sumPaymentsByStudentAndFee(e.getStudent().getId(), e.getFee().getId()))
                    .orElse(BigDecimal.ZERO);

            return StudentFeeMapper.toDTO(e, amountPaid);
        });
    }
}
