package com.moriba.skultem.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FeeDetailTest {

    private static String status(String total, String paid, String balance) {
        return FeeDetail.combined(new BigDecimal(total), new BigDecimal(paid), new BigDecimal(balance)).status();
    }

    @Test
    void aGroupsStatusComesFromItsTotals() {
        // Nursery 3 on production: some paid, most still owed.
        assertThat(status("78000", "27150", "50850")).isEqualTo("Partial");
        assertThat(status("78000", "0", "78000")).isEqualTo("Pending");
        assertThat(status("78000", "78000", "0")).isEqualTo("Paid");
        assertThat(status("0", "0", "0")).isEqualTo("Paid");
    }
}
