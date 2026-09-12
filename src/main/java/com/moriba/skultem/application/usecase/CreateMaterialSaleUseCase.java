package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialSaleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.MaterialSaleMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.MaterialSale;
import com.moriba.skultem.domain.model.MaterialSale.PaymentMethod;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Supply;
import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.model.Transaction.TransactionType;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A sale needs an active academic year/term only to post its ledger entry (see the try/catch
// below) - a school that hasn't set one up yet can still sell a uniform, it just won't show up on
// the Transactions report until they do. dontRollbackOn keeps that catch effective: without it,
// createTransactionUsercase's own @Transactional marks this shared transaction rollback-only the
// moment NotFoundException crosses its proxy boundary, and the sale itself would still roll back
// at commit (UnexpectedRollbackException) even though this method "handled" the exception.
@Service
@Transactional(dontRollbackOn = NotFoundException.class)
@RequiredArgsConstructor
public class CreateMaterialSaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateMaterialSaleUseCase.class);

    private final MaterialSaleRepository repo;
    private final MaterialRepository materialRepo;
    private final StudentRepository studentRepo;
    private final SupplyRepository supplyRepo;
    private final SupplyMaterialUseCase supplyMaterialUseCase;
    private final CreateTransactionUsercase createTransactionUsercase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "MATERIAL_SALE_CREATED")
    public MaterialSaleDTO execute(String schoolId, String materialId, String studentId, String customerName,
            int quantity, BigDecimal unitPrice, BigDecimal amountPaid, PaymentMethod paymentMethod, String note,
            boolean collectNow) {

        var material = materialRepo.findByIdAndSchool(materialId, schoolId)
                .orElseThrow(() -> new NotFoundException("material not found"));

        Student student = null;
        if (studentId != null && !studentId.isBlank()) {
            student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                    .orElseThrow(() -> new NotFoundException("student not found"));
        }

        boolean stockAvailable = material.getStockQuantity().compareTo(java.math.BigInteger.valueOf(quantity)) >= 0;

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        boolean fullyPaid = amountPaid != null && amountPaid.compareTo(total) >= 0;

        // Being in stock, being handed over right now, and being fully paid are three different
        // things - a parent can pay in full today and still ask to collect next week, and a
        // partial payment should never leave with the item no matter how eager the buyer is to
        // collect it now. All three have to hold before the linked Supply is actually collected;
        // otherwise it's left PENDING ("settle later"), same as a genuine out-of-stock pre-sale.
        boolean fulfilledNow = stockAvailable && collectNow && fullyPaid;

        String saleId = UUID.randomUUID().toString();

        // The sale never deducts stock itself - it's always backed by a Supply, the one place
        // collection (and therefore stock deduction) actually happens. sourceSaleId lets
        // GetPendingPickupsUseCase and CancelMaterialSaleUseCase trace back to this sale.
        // Built (but not saved) before the sale purely to mint its id - supplies.source_sale_id is
        // a foreign key to material_sales, so the sale row must be INSERTed first. Saving the
        // supply before the sale it points to had Hibernate's auto-flush (triggered by
        // supplyMaterialUseCase's lookup below) emit that INSERT first and fail the FK check.
        var supply = Supply.create(schoolId, student, customerName, material, quantity, saleId);

        var domain = MaterialSale.create(saleId, schoolId, student, customerName, material, quantity, unitPrice,
                amountPaid, paymentMethod, note, fulfilledNow, supply.getId());
        repo.save(domain);
        supplyRepo.save(supply);

        if (fulfilledNow) {
            // Reuses the exact same collection path a fee-entitled Supply already goes through -
            // one place deducts stock, whether it's a sale or a paid fee behind it.
            supplyMaterialUseCase.execute(schoolId, supply.getId(), quantity,
                    "Sale" + (student != null ? " to " + student.getName() : ""));
        }

        // Post to the general ledger only for money actually collected right now - a pre-sale
        // recorded with amountPaid=0 hasn't moved any cash yet, so there's nothing to post until a
        // payment against it is actually recorded (see RecordMaterialSalePaymentUseCase). Best
        // effort: a school with no academic year/term set up yet can still record the sale itself -
        // it just won't appear on the Transactions report until they set one up.
        if (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            try {
                createTransactionUsercase.createEntry(schoolId, TransactionType.SALE, Transaction.Direction.CREDIT,
                        amountPaid, domain.getId(), ReferenceType.MATERIAL_SALE);
            } catch (NotFoundException ex) {
                log.warn("Could not post ledger entry for material sale {}: {}", domain.getId(), ex.getMessage());
            }
        }

        String buyer = student != null ? student.getName() : domain.getCustomerName();
        String title;
        if (fulfilledNow) {
            title = "Material sold";
        } else if (!stockAvailable) {
            title = "Material pre-sold - awaiting stock";
        } else if (!fullyPaid) {
            title = "Material sold - awaiting full payment";
        } else {
            title = "Material sold - collecting later";
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.SALE,
                title,
                buyer + " · " + quantity + " " + material.getName(),
                null,
                domain.getId());

        return MaterialSaleMapper.toDTO(domain);
    }
}
