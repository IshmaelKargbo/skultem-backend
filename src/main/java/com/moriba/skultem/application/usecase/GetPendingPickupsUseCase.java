package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PendingPickupDTO;
import com.moriba.skultem.application.dto.PendingPickupDTO.Source;
import com.moriba.skultem.domain.model.Supply;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;
import com.moriba.skultem.domain.repository.SupplyRepository;

import lombok.RequiredArgsConstructor;

// Every uncollected Supply - fee-entitled or sale-linked (see Supply#sourceSaleId) - is "something
// somebody still needs to collect", so this reads Supply alone rather than separately merging in
// MaterialSale: a sale never deducts stock or tracks collection itself any more, its linked Supply
// does (see CreateMaterialSaleUseCase), so counting both would double every pending sale.
@Service
@RequiredArgsConstructor
public class GetPendingPickupsUseCase {

    private final SupplyRepository supplyRepo;
    private final MaterialSaleRepository materialSaleRepo;

    public Page<PendingPickupDTO> execute(String schoolId, int page, int size) {
        Pageable pageable = size > 0 ? PageRequest.of(Math.max(page - 1, 0), size) : Pageable.unpaged();

        return supplyRepo.findUncollectedBySchool(schoolId, pageable)
                .map(s -> toDTO(s, schoolId));
    }

    private PendingPickupDTO toDTO(Supply s, String schoolId) {
        boolean fromSale = s.getSourceSaleId() != null;

        // A fee-entitled Supply only exists once its bundled fee is fully paid (see
        // RecordPaymentUseCase#processSupply), so it's always effectively "paid". A sale-linked one
        // carries its own, possibly partial, payment - look the sale up for its actual status.
        String paymentStatus = "PAID";
        if (fromSale) {
            paymentStatus = materialSaleRepo.findByIdAndSchool(s.getSourceSaleId(), schoolId)
                    .map(sale -> sale.getPaymentStatus().name())
                    .orElse("UNKNOWN");
        }

        return new PendingPickupDTO(
                s.getId(),
                fromSale ? Source.SALE : Source.SUPPLY,
                s.getBuyerName(),
                s.getStudent() != null ? s.getStudent().getAdmissionNumber() : null,
                s.getStudent() != null ? s.getStudent().getPhoto() : null,
                s.getMaterial().getName(),
                s.getMaterial().getCategory().getName(),
                s.getQty(),
                s.getCollectedQty(),
                paymentStatus,
                s.getCreatedAt());
    }
}
