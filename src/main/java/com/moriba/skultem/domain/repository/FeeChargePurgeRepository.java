package com.moriba.skultem.domain.repository;

import java.math.BigDecimal;
import java.util.List;

// Takes a fee structure back off students it was charged to - when it is deleted (see
// DeleteFeeStructureUseCase), or edited so some students no longer qualify (see ReconcileFeeChargesUseCase).
public interface FeeChargePurgeRepository {

    // Payments recorded against this fee, by any student.
    long countPayments(String schoolId, String feeId);

    // Who is charged this fee, and how much each has paid towards it.
    List<Charge> charges(String schoolId, String feeId);

    // Removes every student's charge, their discounts and the matching ledger entries. Returns the students
    // whose ledger changed, so their running balances can be recomputed.
    List<String> removeCharges(String schoolId, String feeId);

    // Same, for these students only.
    void removeCharges(String schoolId, String feeId, List<String> studentIds);

    record Charge(String studentId, String enrollmentId, BigDecimal paid) {
    }

    // Charges in open academic years where a "new students only" fee sits on an old student, or an
    // "old students only" fee on a new one (see FeeStructure#appliesToEnrollmentType).
    List<WrongGroupCharge> wrongGroupCharges(String schoolId);

    record WrongGroupCharge(String studentId, String studentName, String admissionNumber, String enrollmentType,
            String className, String feeId, String feeName, String termName, boolean newStudentsOnly,
            BigDecimal amount, BigDecimal paid, boolean discounted) {
    }
}
