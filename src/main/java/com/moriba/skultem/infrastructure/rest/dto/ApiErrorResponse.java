package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        Map<String, Object> details
) {
}
