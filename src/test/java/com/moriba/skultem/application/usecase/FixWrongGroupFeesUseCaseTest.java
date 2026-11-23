package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.repository.FeeChargePurgeRepository;
import com.moriba.skultem.domain.repository.FeeChargePurgeRepository.WrongGroupCharge;

class FixWrongGroupFeesUseCaseTest {

    private final FeeChargePurgeRepository repo = mock(FeeChargePurgeRepository.class);
    private final RecomputeStudentLedgerBalancesUseCase recompute = mock(RecomputeStudentLedgerBalancesUseCase.class);
    private final LogActivityUseCase log = mock(LogActivityUseCase.class);
    private final FixWrongGroupFeesUseCase useCase = new FixWrongGroupFeesUseCase(repo, recompute, log);

    private static WrongGroupCharge charge(String student, String fee, String paid, boolean discounted) {
        return new WrongGroupCharge(student, "Fatu Kamara", "A1", "RE_ENROLLMENT", "Nursery 3", fee, "Admission",
                "Term 1", true, new BigDecimal("500"), new BigDecimal(paid), discounted);
    }

    @Test
    void previewChangesNothing() {
        when(repo.wrongGroupCharges("s")).thenReturn(List.of(charge("st1", "f1", "0", false)));
        var res = useCase.preview("s");
        assertThat(res.removed()).isEqualTo(1);
        assertThat(res.items().get(0).feeFor()).isEqualTo("New students only");
        verify(repo, never()).removeCharges(anyString(), anyString(), any());
    }

    @Test
    void removesUnpaidChargesAndLeavesPaidOrDiscountedOnes() {
        when(repo.wrongGroupCharges("s")).thenReturn(List.of(
                charge("st1", "f1", "0", false),
                charge("st2", "f1", "0", false),
                charge("st3", "f1", "200", false),
                charge("st4", "f2", "0", true)));

        var res = useCase.fix("s");

        assertThat(res.removed()).isEqualTo(2);
        assertThat(res.needsAttention()).isEqualTo(2);
        verify(repo).removeCharges("s", "f1", List.of("st1", "st2"));
        verify(repo, never()).removeCharges(anyString(), org.mockito.ArgumentMatchers.eq("f2"), any());
        verify(recompute).recomputeForStudent("st1", "s");
        verify(recompute).recomputeForStudent("st2", "s");
        verify(recompute, never()).recomputeForStudent("st3", "s");
        assertThat(res.items()).filteredOn(i -> !i.canRemove()).extracting(i -> i.reason())
                .allMatch(r -> r.contains("payments") || r.contains("discount"));
    }
}
