package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.MaterialTransactionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a material the school no longer stocks. Blocked once it has any restock or supply
 * history - removing it would orphan those transaction records and any supply given against it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteMaterialUseCase {

    private final MaterialRepository repo;
    private final MaterialTransactionRepository materialTransactionRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_DELETED")
    public void execute(String schoolId, String id) {
        var material = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Material not found"));

        if (materialTransactionRepo.existsByMaterialAndSchool(id, schoolId)) {
            throw new RuleException(
                    "This material has restock or supply history and can't be deleted.");
        }

        repo.deleteById(id);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Material deleted",
                material.getName(),
                null,
                id);
    }
}
