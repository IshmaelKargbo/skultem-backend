package com.moriba.skultem.application.dto;

import java.time.Instant;

// One row in the combined "who still needs to collect something" view - merges fee-entitled
// Supply records with unfulfilled MaterialSales without touching either's own write-side logic
// (each keeps deducting stock and tracking its own status independently - see
// GetPendingPickupsUseCase for why they aren't merged at the data-model level).
public record PendingPickupDTO(
        String id,
        Source source,
        String buyerName,
        String admissionNumber,
        String buyerPhoto,
        String materialName,
        String categoryName,
        int quantity,
        int collectedQuantity,
        String paymentStatus,
        Instant since) {

    public enum Source {
        SUPPLY,
        SALE
    }
}
