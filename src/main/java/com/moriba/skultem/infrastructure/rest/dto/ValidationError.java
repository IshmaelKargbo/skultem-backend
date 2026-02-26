package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationError(int status,
        String message,
        LocalDateTime timestamp,
        Map<String, String> errors) {
}
