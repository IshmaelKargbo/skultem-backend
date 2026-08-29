package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PaymentMapper;
import com.moriba.skultem.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

// Re-fetches every payment recorded under one receipt number so a past receipt can be viewed or
// re-downloaded exactly as it looked at record time - a single "Record Payment" submission can
// spread across several PaymentRecord rows (one per fee it was allocated to) that all share the
// receipt number generated for that submission.
@Service
@RequiredArgsConstructor
public class GetReceiptUseCase {
    private final PaymentRepository repo;

    public List<PaymentDTO> execute(String schoolId, String referenceNo) {
        var payments = repo.findAllByReferenceNoAndSchoolId(referenceNo, schoolId);

        if (payments.isEmpty()) {
            throw new NotFoundException("receipt not found");
        }

        return payments.stream().map(PaymentMapper::toDTO).toList();
    }
}
