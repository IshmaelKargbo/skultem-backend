package com.moriba.skultem.domain.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.MaterialSale;
import com.moriba.skultem.domain.model.MaterialSale.Status;

public interface MaterialSaleRepository {
    void save(MaterialSale domain);

    Optional<MaterialSale> findByIdAndSchool(String id, String schoolId);

    Page<MaterialSale> findByStudentAndSchool(String studentId, String schoolId, Pageable pageable);

    // Backs the sales list. `query`/`status` are nullable (each condition is skipped when its
    // param is null); `paidPending` layers on top of whichever of those is set, meaning "paid
    // (fully or partially) but not yet collected" - the segment a school actually needs to chase
    // down, since real money is already sitting against an unfulfilled obligation. Search, status
    // and paidPending all combine rather than search silently overriding the active status tab.
    Page<MaterialSale> filter(String schoolId, String query, Status status, boolean paidPending, Pageable pageable);

    long countBySchool(String schoolId);

    long countBySchoolAndStatus(String schoolId, Status status);

    long countAwaitingCollectionWithPayment(String schoolId);

    // Excludes CANCELLED sales - money never collected/owed on a cancelled sale shouldn't inflate
    // either total.
    BigDecimal sumAmountPaidBySchool(String schoolId);

    BigDecimal sumOutstandingBalanceBySchool(String schoolId);
}
