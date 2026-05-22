package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SupplyMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.MaterialTransaction;
import com.moriba.skultem.domain.model.MaterialTransaction.Direction;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.MaterialTransactionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SupplyMaterialUseCase {

    private final SupplyRepository repo;
    private final MaterialRepository materialRepo;
    private final MaterialTransactionRepository materialTransactionRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SUPPLY_MATERIAL")
    public SupplyDTO execute(String schoolId, String id, int qty, String note) {

        var domain = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("supply not found"));

        int remaining = domain.getQty() - domain.getCollectedQty();

        if (remaining <= 0) {
            throw new IllegalStateException("No remaining quantity to collect");
        }

        int qtyToCollect = Math.min(qty, remaining);

        // update domain
        domain.collect(qtyToCollect);
        repo.save(domain);

        // transaction log
        var mt = MaterialTransaction.create(
                schoolId,
                domain.getMaterial(),
                qtyToCollect,
                Direction.OUT,
                note
        );

        materialTransactionRepo.save(mt);
        var material = domain.getMaterial();
        material.deduct(qty);
        materialRepo.save(material);

        // activity log
        logActivityUseCase.log(
                schoolId,
                ActivityType.SUPPLY,
                "Material supplied successfully",
                domain.getStudent().getName(),
                null,
                domain.getId()
        );

        // return updated DTO
        return SupplyMapper.toDTO(domain);
    }
}