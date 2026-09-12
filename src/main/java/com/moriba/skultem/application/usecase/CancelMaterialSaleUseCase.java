package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialSaleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.MaterialSaleMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Only a sale still PENDING_SUPPLY can be cancelled here - its linked Supply hasn't collected
// anything yet, so there's no stock to reverse; cancelling it too keeps it off both the Supply
// list and Pending Pickups. A FULFILLED sale already handed the item over; unwinding that is a
// return/refund, out of scope for this use case.
@Service
@Transactional
@RequiredArgsConstructor
public class CancelMaterialSaleUseCase {

    private final MaterialSaleRepository repo;
    private final SupplyRepository supplyRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_SALE_CANCELLED")
    public MaterialSaleDTO execute(String schoolId, String id) {
        var domain = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("sale not found"));

        try {
            domain.cancel();
        } catch (IllegalStateException ex) {
            throw new RuleException(ex.getMessage());
        }

        repo.save(domain);

        supplyRepo.findByIdAndSchool(domain.getSupplyId(), schoolId).ifPresent(supply -> {
            try {
                supply.cancel();
                supplyRepo.save(supply);
            } catch (IllegalStateException ex) {
                // Already collected/cancelled somehow despite the sale itself still being
                // PENDING_SUPPLY - leave it as-is rather than fail the sale cancellation over it.
            }
        });

        String buyer = domain.getStudent() != null ? domain.getStudent().getName() : domain.getCustomerName();

        logActivityUseCase.log(
                schoolId,
                ActivityType.SALE,
                "Sale cancelled",
                buyer + " · " + domain.getQuantity() + " " + domain.getMaterial().getName(),
                null,
                domain.getId());

        return MaterialSaleMapper.toDTO(domain);
    }
}
