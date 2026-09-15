package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.Material;
import com.moriba.skultem.domain.model.MaterialCategory;
import com.moriba.skultem.domain.model.MaterialSale;
import com.moriba.skultem.domain.model.MaterialSale.PaymentMethod;
import com.moriba.skultem.domain.model.MaterialSale.Status;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;

// Regression coverage for the production 500 traced to SupplyMaterialUseCase's "No remaining
// quantity to collect" - caused by two concurrent "Fulfill" requests for the same pre-sold item
// (e.g. a double-click) both passing the PENDING_SUPPLY guard before either commits. See
// FulfillMaterialSaleUseCase for the fix.
class FulfillMaterialSaleUseCaseTest {

    private final MaterialSaleRepository repo = mock(MaterialSaleRepository.class);
    private final SupplyMaterialUseCase supplyMaterialUseCase = mock(SupplyMaterialUseCase.class);
    private final LogActivityUseCase logActivityUseCase = mock(LogActivityUseCase.class);
    private final FulfillMaterialSaleUseCase useCase = new FulfillMaterialSaleUseCase(repo, supplyMaterialUseCase,
            logActivityUseCase);

    private Material material(int stock) {
        var category = MaterialCategory.create("school-1", "Uniforms", null);
        return Material.create("school-1", "Sweater", Material.Unit.PCS, BigInteger.valueOf(stock),
                BigDecimal.TEN, category);
    }

    private MaterialSale pendingSale(Material material, int qty, String supplyId) {
        return MaterialSale.create("sale-1", "school-1", null, "Walk-in buyer", material, qty, BigDecimal.TEN,
                BigDecimal.ZERO, PaymentMethod.CASH, null, false, supplyId);
    }

    @Test
    void aDuplicateFulfillIsTreatedAsSuccessOnceTheOtherRequestAlreadyCompletedIt() {
        var material = material(10);
        var sale = pendingSale(material, 5, "supply-1");

        // The other, concurrent request already collected the whole linked Supply and flipped
        // this sale to FULFILLED - simulate that arriving as the "no remaining quantity" guard.
        when(supplyMaterialUseCase.execute(anyString(), anyString(), anyInt(), any()))
                .thenThrow(new IllegalStateException("No remaining quantity to collect"));

        var fulfilledElsewhere = MaterialSale.create("sale-1", "school-1", null, "Walk-in buyer", material, 5,
                BigDecimal.TEN, BigDecimal.ZERO, PaymentMethod.CASH, null, true, "supply-1");

        // First lookup (top of the method) sees the stale PENDING_SUPPLY that let this request
        // proceed at all; the re-read after the exception picks up the fresher, already-FULFILLED
        // state written by the other request.
        when(repo.findByIdAndSchool("sale-1", "school-1"))
                .thenReturn(java.util.Optional.of(sale))
                .thenReturn(java.util.Optional.of(fulfilledElsewhere));

        var result = useCase.execute("school-1", "sale-1", null);

        assertThat(result.status()).isEqualTo(Status.FULFILLED);
        // A duplicate request shouldn't add a second activity log entry for the same fulfillment.
        verifyNoMoreInteractions(logActivityUseCase);
    }

    @Test
    void aGenuineFailureFromSupplyCollectionStillPropagates() {
        var material = material(10);
        var sale = pendingSale(material, 5, "supply-1");

        when(repo.findByIdAndSchool("sale-1", "school-1")).thenReturn(java.util.Optional.of(sale));
        when(supplyMaterialUseCase.execute(anyString(), anyString(), anyInt(), any()))
                .thenThrow(new IllegalStateException("Cannot collect cancelled supply request"));

        assertThatThrownBy(() -> useCase.execute("school-1", "sale-1", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot collect cancelled supply request");
    }
}
