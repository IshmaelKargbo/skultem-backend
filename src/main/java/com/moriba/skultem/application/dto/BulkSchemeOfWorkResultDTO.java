package com.moriba.skultem.application.dto;

import java.util.List;

public record BulkSchemeOfWorkResultDTO(
        int created,
        int skipped,
        int failed,
        List<BulkSchemeRowResultDTO> rows) {
}
