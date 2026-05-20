package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SupplyMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CollectSupplyUseCase {
    private final SupplyRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SUPPLY_COLLECTED")
    public SupplyDTO execute(String schoolId, String id, int qty) {
        var domain = repo.findByIdAndSchool(id, schoolId).orElseThrow(() -> new NotFoundException("supply not found"));
        domain.collect(qty);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.FEES,
                "Supply collected",
                domain.getStudent().getName() + " " + domain.getCollectedQty() + " of " + domain.getQty(),
                null,
                domain.getId());

        return SupplyMapper.toDTO(domain);
    }
}
