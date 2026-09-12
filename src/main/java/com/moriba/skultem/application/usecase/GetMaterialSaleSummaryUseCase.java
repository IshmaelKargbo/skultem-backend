package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialSaleSummaryDTO;
import com.moriba.skultem.domain.model.MaterialSale.Status;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMaterialSaleSummaryUseCase {

    private final MaterialSaleRepository repo;

    public MaterialSaleSummaryDTO execute(String schoolId) {
        long total = repo.countBySchool(schoolId);
        long pending = repo.countBySchoolAndStatus(schoolId, Status.PENDING_SUPPLY);

        return new MaterialSaleSummaryDTO(
                total,
                pending,
                repo.sumAmountPaidBySchool(schoolId),
                repo.sumOutstandingBalanceBySchool(schoolId),
                repo.countAwaitingCollectionWithPayment(schoolId));
    }
}
