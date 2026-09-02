package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ClockInResponseDTO(boolean alreadyClockedIn, Instant clockedInAt, double distanceMeters) {
}
