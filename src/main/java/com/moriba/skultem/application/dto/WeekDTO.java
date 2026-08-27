package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record WeekDTO(String id, String schemeId, int week, String topic, String subTopic, List<String> objectives, String state, Instant createdAt,
                Instant updatedAt) {
}
