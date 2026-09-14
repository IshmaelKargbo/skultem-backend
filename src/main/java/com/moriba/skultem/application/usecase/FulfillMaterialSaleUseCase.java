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
@Service
@Transactional
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

        supplyMaterialUseCase.execute(schoolId, domain.getSupplyId(), domain.getQuantity(),
                note != null && !note.isBlank() ? note : "Pre-sold item fulfilled");

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
