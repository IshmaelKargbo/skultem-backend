package com.moriba.skultem.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.services.BillingService.State;

class BillingRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);

    @Test
    void aPeriodEndsTheDayBeforeTheNextOneStarts() {
        assertEquals(LocalDate.of(2026, 11, 15), BillingService.periodEnd(LocalDate.of(2026, 10, 16), "MONTHLY"));
        assertEquals(LocalDate.of(2027, 1, 14), BillingService.periodEnd(LocalDate.of(2026, 10, 15), "QUARTERLY"));
        assertEquals(LocalDate.of(2027, 10, 15), BillingService.periodEnd(LocalDate.of(2026, 10, 16), "ANNUAL"));
        // month ends: 31 January + 1 month is 28 February (the last day of the month), less a day
        assertEquals(LocalDate.of(2026, 2, 27), BillingService.periodEnd(LocalDate.of(2026, 1, 31), "MONTHLY"));
    }

    @Test
    void aCancelledSubscriptionIsCancelledWhateverElseIsTrue() {
        assertEquals(State.CANCELLED, BillingService.stateOf(true, true, true, TODAY.plusDays(5), TODAY));
    }

    @Test
    void anOverdueInvoiceMakesItOverdueEvenInsideAPaidPeriod() {
        assertEquals(State.OVERDUE, BillingService.stateOf(false, true, true, TODAY.plusDays(5), TODAY));
    }

    @Test
    void aPaidInvoiceCoveringTodayIsActive() {
        assertEquals(State.ACTIVE, BillingService.stateOf(false, false, true, TODAY.plusDays(5), TODAY));
    }

    @Test
    void whenTheInvoicedPeriodsHaveRunOutItHasExpired() {
        assertEquals(State.EXPIRED, BillingService.stateOf(false, false, false, TODAY.minusDays(1), TODAY));
    }

    @Test
    void aSubscriptionNotYetPaidForIsPending() {
        assertEquals(State.PENDING, BillingService.stateOf(false, false, false, TODAY.plusDays(20), TODAY));
        // a period ending today is still running
        assertEquals(State.PENDING, BillingService.stateOf(false, false, false, TODAY, TODAY));
    }
}
