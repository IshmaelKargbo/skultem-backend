package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ClockOutResponseDTO(boolean alreadyClockedOut, Instant clockedOutAt, double distanceMeters) {
}
