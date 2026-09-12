package com.moriba.skultem.application.usecase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.events.SupplyCollectedEvent;
import com.moriba.skultem.application.mapper.SupplyMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.MaterialTransaction;
import com.moriba.skultem.domain.model.MaterialTransaction.Direction;
import com.moriba.skultem.domain.model.Supply;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.MaterialTransactionRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The one place a material actually changes hands and stock is deducted - whether the Supply
// being collected here came from a paid fee or a MaterialSale (see Supply#sourceSaleId). Both a
// fee-entitled collection and a sale fulfillment go through this exact method.
@Service
@Transactional
@RequiredArgsConstructor
public class SupplyMaterialUseCase {

    private final SupplyRepository repo;
    private final MaterialRepository materialRepo;
    private final MaterialTransactionRepository materialTransactionRepo;
    private final StudentParentRepository studentParentRepo;
    private final ApplicationEventPublisher eventPublisher;
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
        // Deduct exactly what was actually collected (capped above), not the raw request - a staff
        // member typing a qty larger than what's left over-deducted stock by the difference while
        // under-recording what the student actually received.
        material.deduct(qtyToCollect);
        materialRepo.save(material);

        // activity log
        logActivityUseCase.log(
                schoolId,
                ActivityType.SUPPLY,
                "Material supplied successfully",
                domain.getBuyerName(),
                null,
                domain.getId()
        );

        // Let the student's parent(s) know - best effort, since a walk-in buyer has no student to
        // notify and a student without a linked parent account simply has nobody to tell. Neither
        // should ever fail the collection itself.
        notifyParents(domain, qtyToCollect);

        // return updated DTO
        return SupplyMapper.toDTO(domain);
    }

    private void notifyParents(Supply domain, int qtyCollected) {
        if (domain.getStudent() == null) {
            return;
        }

        Set<String> seenUserIds = new HashSet<>();
        List<User> parentUsers = studentParentRepo
                .findAllByStudentAndSchool(domain.getStudent().getId(), domain.getSchoolId())
                .stream()
                .map(studentParent -> studentParent.getParent())
                .filter(parent -> parent != null && parent.getUser() != null)
                .map(parent -> parent.getUser())
                .filter(user -> seenUserIds.add(user.getId()))
                .toList();

        for (User user : parentUsers) {
            eventPublisher.publishEvent(new SupplyCollectedEvent(
                    domain.getSchoolId(),
                    user,
                    domain.getStudent().getName(),
                    domain.getMaterial().getName(),
                    qtyCollected,
                    domain.getStatus() == Supply.Status.COLLECTED,
                    null));
        }
    }
}
