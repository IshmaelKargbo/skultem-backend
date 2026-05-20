package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.MaterialMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.MaterialTransaction;
import com.moriba.skultem.domain.model.MaterialTransaction.Direction;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.MaterialTransactionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReStockMaterialUseCase {
    private final MaterialRepository repo;
    private final MaterialTransactionRepository materialTransactionRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_RESTOCK")
    public MaterialDTO execute(String schoolId, String id, int qty, String note) {
        var domain = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("material not found"));

        domain.stock(qty);
        repo.save(domain);

        var mt = MaterialTransaction.create(schoolId, domain, qty, Direction.IN, note);
        materialTransactionRepo.save(mt);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Material restock successfully",
                domain.getName(),
                null,
                domain.getId());

        return MaterialMapper.toDTO(domain);
    }
}
