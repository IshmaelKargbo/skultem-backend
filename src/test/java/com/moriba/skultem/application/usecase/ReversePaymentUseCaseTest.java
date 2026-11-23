package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.FeeCategory;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.Payment;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.PaymentReversalRepository;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

class ReversePaymentUseCaseTest {

    private final PaymentRepository paymentRepo = mock(PaymentRepository.class);
    private final PaymentReversalRepository reversalRepo = mock(PaymentReversalRepository.class);
    private final StudentLedgerEntryRepository ledgerRepo = mock(StudentLedgerEntryRepository.class);
    private final RecomputeStudentLedgerBalancesUseCase recompute = mock(RecomputeStudentLedgerBalancesUseCase.class);
    private final CreateTransactionUsercase transactions = mock(CreateTransactionUsercase.class);
    private final LogActivityUseCase log = mock(LogActivityUseCase.class);
    private final ReversePaymentUseCase useCase = new ReversePaymentUseCase(paymentRepo, reversalRepo, ledgerRepo,
            recompute, transactions, log);

    private Payment payment(boolean system) {
        var student = mock(Student.class);
        when(student.getId()).thenReturn("st1");
        when(student.getGivenNames()).thenReturn("Fatu");
        when(student.getFamilyName()).thenReturn("Kamara");
        var fee = mock(FeeStructure.class);
        when(fee.isSystem()).thenReturn(system);
        when(fee.getCategory()).thenReturn(FeeCategory.create("c", "s", "Tuition", null));
        var term = mock(Term.class);
        when(term.getName()).thenReturn("First Term");
        when(fee.getTerm()).thenReturn(term);
        var p = mock(Payment.class);
        when(p.getId()).thenReturn("p1");
        when(p.getStudent()).thenReturn(student);
        when(p.getFee()).thenReturn(fee);
        when(p.getAmount()).thenReturn(new BigDecimal("500"));
        when(p.getReferenceNo()).thenReturn("RCT-1");
        return p;
    }

    private StudentLedgerEntry entry(TransactionType type, String ref) {
        var e = mock(StudentLedgerEntry.class);
        when(e.getTransactionType()).thenReturn(type);
        when(e.getReferenceId()).thenReturn(ref);
        return e;
    }

    @Test
    @SuppressWarnings("unchecked")
    void removesThePaymentAndItsLedgerCreditAndBooksAnAdjustment() {
        var p = payment(false);
        when(paymentRepo.findByIdAndSchoolId("p1", "s")).thenReturn(Optional.of(p));
        var credit = entry(TransactionType.PAYMENT, "p1");
        var charge = entry(TransactionType.FEE_ASSINMENT, "fee1");
        var otherPayment = entry(TransactionType.PAYMENT, "p2");
        when(ledgerRepo.findAllByStudentIdAndSchoolIdOrderByPaidAtAscCreatedAtAsc("st1", "s"))
                .thenReturn(List.of(charge, credit, otherPayment));

        var res = useCase.execute("s", "st1", "p1", "Recorded by mistake", "u1");

        verify(reversalRepo).record(eq(p), eq("Recorded by mistake"), eq("u1"), any());
        ArgumentCaptor<List<StudentLedgerEntry>> deleted = ArgumentCaptor.forClass(List.class);
        verify(ledgerRepo).deleteAll(deleted.capture());
        assertThat(deleted.getValue()).containsExactly(credit);
        verify(paymentRepo).delete(p);
        verify(recompute).recomputeForStudent("st1", "s");
        verify(transactions).createEntry("s", Transaction.TransactionType.ADJUSTMENT, Transaction.Direction.DEBIT,
                new BigDecimal("500"), "p1", Transaction.ReferenceType.STUDENT);
        assertThat(res.fee()).isEqualTo("Tuition (First Term)");
    }

    @Test
    void needsAReasonAndTheRightStudent() {
        var p = payment(false);
        when(paymentRepo.findByIdAndSchoolId("p1", "s")).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> useCase.execute("s", "st1", "p1", " ", "u1")).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> useCase.execute("s", "other", "p1", "mistake", "u1"))
                .hasMessageContaining("not found");
        verify(paymentRepo, never()).delete(any());
        verify(ledgerRepo, never()).deleteAll(anyList());
    }

    @Test
    void leavesThePlatformFeeAlone() {
        var p = payment(true);
        when(paymentRepo.findByIdAndSchoolId("p1", "s")).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> useCase.execute("s", "st1", "p1", "mistake", "u1"))
                .isInstanceOf(RuleException.class).hasMessageContaining("Platform fee");
        verify(paymentRepo, never()).delete(any());
    }
}
