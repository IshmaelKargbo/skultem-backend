package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.vo.FeeInstallment;
import com.moriba.skultem.domain.vo.InstallmentStatus.State;

class FeeInstallmentPlanTest {

    private static final LocalDate OCT = LocalDate.of(2026, 10, 15);
    private static final LocalDate JAN = LocalDate.of(2027, 1, 15);
    private static final LocalDate APR = LocalDate.of(2027, 4, 15);

    private FeeStructure fee(String amount, boolean installments) {
        return FeeStructure.create("s", FeeStructure.Type.ALL, null, false, List.of(), null, null, null, APR,
                new BigDecimal(amount), "Tuition", installments, false, false, null, null);
    }

    private List<FeeInstallment> threeParts() {
        return List.of(new FeeInstallment("1st", new BigDecimal("500"), OCT),
                new FeeInstallment("2nd", new BigDecimal("300"), JAN),
                new FeeInstallment(null, new BigDecimal("200"), APR));
    }

    @Test
    void aPlanMustAddUpToTheFeeAndNeedsInstallmentsOn() {
        assertThatThrownBy(() -> fee("1000", false).setInstallmentPlan(threeParts()))
                .isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> fee("900", true).setInstallmentPlan(threeParts()))
                .isInstanceOf(RuleException.class).hasMessageContaining("add up");
        assertThatThrownBy(() -> fee("500", true).setInstallmentPlan(
                List.of(new FeeInstallment("only", new BigDecimal("500"), OCT))))
                .isInstanceOf(RuleException.class);
    }

    @Test
    void aPlanSetsTheFeeDueDateToItsLastPartAndNamesUnnamedParts() {
        var fee = fee("1000", true);
        fee.setInstallmentPlan(threeParts());
        assertThat(fee.getDueDate()).isEqualTo(APR);
        assertThat(fee.getInstallments().get(2).label()).isEqualTo("3rd installment");
    }

    @Test
    void overdueOnlyWhenAPartThatFellDueIsntCovered() {
        var fee = fee("1000", true);
        fee.setInstallmentPlan(threeParts());
        var november = LocalDate.of(2026, 11, 1);
        assertThat(fee.isOverdue(new BigDecimal("500"), BigDecimal.ZERO, november)).isFalse();
        assertThat(fee.isOverdue(new BigDecimal("400"), BigDecimal.ZERO, november)).isTrue();
        assertThat(fee.isOverdue(BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 10, 1))).isFalse();
    }

    @Test
    void paymentsFillThePartsInOrderAndDiscountsComeOffEachPart() {
        var fee = fee("1000", true);
        fee.setInstallmentPlan(threeParts());
        var statuses = fee.installmentStatuses(new BigDecimal("600"), new BigDecimal("100"),
                LocalDate.of(2026, 11, 1));
        // 10% off: 450 / 270 / 180.
        assertThat(statuses).extracting(s -> s.amount().toPlainString()).containsExactly("450.00", "270.00", "180.00");
        assertThat(statuses).extracting(s -> s.state()).containsExactly(State.PAID, State.PARTIAL, State.DUE);
        assertThat(statuses.get(1).paid()).isEqualByComparingTo("150");
    }

    @Test
    void turningInstallmentsOffDropsThePlan() {
        var fee = fee("1000", true);
        fee.setInstallmentPlan(threeParts());
        fee.update(null, null, List.of(), false, APR, new BigDecimal("1000"), "Tuition", false, false, false, null);
        assertThat(fee.hasInstallmentPlan()).isFalse();
    }

    @Test
    void thePlatformFeeIsSplitByTermByDefault() {
        var setting = PlatformFeeSetting.create("s", new BigDecimal("150"));
        var terms = List.of(
                Term.create("t3", "s", null, "Third Term", 3, Term.Status.UPCOMING, JAN.plusDays(30), APR),
                Term.create("t1", "s", null, "First Term", 1, Term.Status.ACTIVE, OCT.minusDays(40), OCT),
                Term.create("t2", "s", null, "Second Term", 2, Term.Status.UPCOMING, OCT.plusDays(30), JAN));
        var plan = setting.planFor(new BigDecimal("100"), terms, null);
        assertThat(plan).extracting(FeeInstallment::label).containsExactly("First Term", "Second Term", "Third Term");
        assertThat(plan).extracting(p -> p.amount().toPlainString()).containsExactly("33.33", "33.33", "33.34");
        assertThat(plan.get(0).dueDate()).isEqualTo(OCT);

        setting.updatePlan(PlatformFeeSetting.InstallmentMode.BY_TERM, List.of(50, 30, 20));
        assertThat(setting.planFor(new BigDecimal("150"), terms, null)).extracting(p -> p.amount().toPlainString())
                .containsExactly("75.00", "45.00", "30.00");

        setting.updatePlan(PlatformFeeSetting.InstallmentMode.FULL, null);
        assertThat(setting.planFor(new BigDecimal("150"), terms, null)).isEmpty();
    }

    @Test
    void thePlatformFeeIsStillThreePartsWhenTheYearDoesntHaveThreeTerms() {
        var setting = PlatformFeeSetting.create("s", new BigDecimal("300"));
        var year = AcademicYear.create("y", "s", "2026/27", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
        var oneTerm = List.of(Term.create("t1", "s", year, "First Term", 1, Term.Status.ACTIVE,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 15)));
        var plan = setting.planFor(new BigDecimal("300"), oneTerm, year);
        assertThat(plan).hasSize(3);
        assertThat(plan).extracting(p -> p.amount().toPlainString()).containsExactly("100.00", "100.00", "100.00");
        assertThat(plan.get(2).dueDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(plan.get(0).dueDate()).isBefore(plan.get(1).dueDate());
    }

    @Test
    void termSharesMustAddUpTo100() {
        var setting = PlatformFeeSetting.create("s", new BigDecimal("150"));
        assertThatThrownBy(() -> setting.updatePlan(null, List.of(50, 30)))
                .isInstanceOf(RuleException.class);
    }

    @Test
    void thePlatformFeeAllowsPartPaymentsOnlyWithAPlan() {
        var fee = FeeStructure.createSystemFee("s", null, null, null, APR, new BigDecimal("150"), "Platform fee");
        fee.applyPlatformPlan(List.of(new FeeInstallment("First Term", new BigDecimal("75"), OCT),
                new FeeInstallment("Second Term", new BigDecimal("75"), JAN)));
        assertThat(fee.isAllowInstallment()).isTrue();
        fee.applyPlatformPlan(List.of());
        assertThat(fee.isAllowInstallment()).isFalse();
        assertThatThrownBy(() -> fee.setInstallmentPlan(List.of())).isInstanceOf(RuleException.class);
    }
}
