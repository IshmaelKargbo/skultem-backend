package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

// Backs the sales page's header cards - avoids the front-end having to page through every sale
// just to add up totals that the database can already sum for it.
public record MaterialSaleSummaryDTO(long totalSales, long pendingSettlement, BigDecimal totalCollected,
        BigDecimal totalOutstanding,
        // Paid (fully or partially) but not yet handed over - the subset of pendingSettlement that
        // actually needs chasing, since real money is already sitting against it.
        long paidAwaitingPickup) {
}
