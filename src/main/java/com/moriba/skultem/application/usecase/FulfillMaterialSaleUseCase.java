package com.moriba.skultem.application.usecase;

import java.math.BigInteger;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialSaleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.MaterialSaleMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.MaterialSale.Status;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Settles a pre-sold ("settle later") sale once stock has actually arrived, by collecting its
// linked Supply - the same mechanism a fee-entitled Supply already uses. See
// CreateMaterialSaleUseCase and Supply#sourceSaleId.
//
// dontRollbackOn: the catch below handles supplyMaterialUseCase's IllegalStateException (a
// concurrent duplicate fulfill), but SupplyMaterialUseCase is its own @Transactional bean joining
// this same transaction - by the time that exception reaches our catch, its proxy has already
// marked the shared transaction rollback-only, so without this we'd "handle" it here and still
// blow up with UnexpectedRollbackException at commit. See CreateMaterialSaleUseCase for the same
// pattern.
@Service
@Transactional(dontRollbackOn = IllegalStateException.class)
@RequiredArgsConstructor
public class FulfillMaterialSaleUseCase {

    private final MaterialSaleRepository repo;
    private final SupplyMaterialUseCase supplyMaterialUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_SALE_FULFILLED")
    public MaterialSaleDTO execute(String schoolId, String id, String note) {
        var domain = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("sale not found"));

        if (domain.getStatus() != Status.PENDING_SUPPLY) {
            throw new RuleException("Only a pending sale awaiting stock can be fulfilled");
        }

        var material = domain.getMaterial();

        if (material.getStockQuantity().compareTo(BigInteger.valueOf(domain.getQuantity())) < 0) {
            throw new RuleException("Not enough stock yet to fulfill this sale. Restock " + material.getName()
                    + " first");
        }

        try {
            supplyMaterialUseCase.execute(schoolId, domain.getSupplyId(), domain.getQuantity(),
                    note != null && !note.isBlank() ? note : "Pre-sold item fulfilled");
        } catch (IllegalStateException ex) {
            // Two concurrent "Fulfill" requests for the same sale (double-click, a retried
            // request) can both pass the PENDING_SUPPLY check above before either commits - the
            // first collects the whole linked Supply and flips this sale to FULFILLED, then the
            // second reaches here to find nothing left to collect. Re-read: if the other request
            // really did already fulfill it, this one is a harmless duplicate - treat it as
            // success instead of surfacing a conflict for something that, from the caller's
            // perspective, already happened. Any other state (still PENDING_SUPPLY) means this
            // genuinely failed, so let it propagate.
            var fresh = repo.findByIdAndSchool(id, schoolId)
                    .orElseThrow(() -> new NotFoundException("sale not found"));

            if (fresh.getStatus() != Status.FULFILLED) {
                throw ex;
            }

            return MaterialSaleMapper.toDTO(fresh);
        }

        // SupplyMaterialUseCase just collected this sale's whole linked Supply, which marks and
        // saves this exact sale FULFILLED itself (see Supply#sourceSaleId there) - re-read it
        // fresh rather than trust this now-stale copy.
        domain = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("sale not found"));

        String buyer = domain.getStudent() != null ? domain.getStudent().getName() : domain.getCustomerName();

        logActivityUseCase.log(
                schoolId,
                ActivityType.SALE,
                "Pre-sold material fulfilled",
                buyer + " · " + domain.getQuantity() + " " + material.getName(),
                null,
                domain.getId());

        return MaterialSaleMapper.toDTO(domain);
    }
}
