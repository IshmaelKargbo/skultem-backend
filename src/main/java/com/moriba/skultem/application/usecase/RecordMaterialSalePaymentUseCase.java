package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialSaleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.MaterialSaleMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.MaterialSale.PaymentMethod;
import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.model.Transaction.TransactionType;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Tops up a sale's amountPaid - covers a pre-sale that only took a deposit up front, settling the
// rest before or after the item itself is fulfilled. dontRollbackOn: see CreateMaterialSaleUseCase
// - the ledger post below can legitimately fail for a school with no active academic year/term
// yet, and that shouldn't roll back the payment that was actually collected.
@Service
@Transactional(dontRollbackOn = NotFoundException.class)
@RequiredArgsConstructor
public class RecordMaterialSalePaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecordMaterialSalePaymentUseCase.class);

    private final MaterialSaleRepository repo;
    private final CreateTransactionUsercase createTransactionUsercase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_SALE_PAYMENT_RECORDED")
    public MaterialSaleDTO execute(String schoolId, String id, BigDecimal amount, PaymentMethod method) {
        var domain = repo.findByIdAndSchool(id, schoolId)
                .orElseThrow(() -> new NotFoundException("sale not found"));

        try {
            domain.recordPayment(amount, method);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new RuleException(ex.getMessage());
        }

        repo.save(domain);

        try {
            createTransactionUsercase.createEntry(schoolId, TransactionType.SALE, Transaction.Direction.CREDIT,
                    amount, domain.getId(), ReferenceType.MATERIAL_SALE);
        } catch (NotFoundException ex) {
            log.warn("Could not post ledger entry for material sale payment {}: {}", domain.getId(),
                    ex.getMessage());
        }

        String buyer = domain.getStudent() != null ? domain.getStudent().getName() : domain.getCustomerName();

        logActivityUseCase.log(
                schoolId,
                ActivityType.SALE,
                "Payment recorded on sale",
                buyer + " paid " + amount,
                null,
                domain.getId());

        return MaterialSaleMapper.toDTO(domain);
    }
}
