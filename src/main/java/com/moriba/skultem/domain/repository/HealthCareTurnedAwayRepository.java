package com.moriba.skultem.domain.repository;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.model.HealthCareTurnedAway;

public interface HealthCareTurnedAwayRepository {

    void save(HealthCareTurnedAway domain);

    /** Turned-away entries between {@code from} (inclusive) and {@code to} (exclusive), newest first. */
    List<HealthCareTurnedAway> findBetween(String schoolId, Instant from, Instant to);

    List<HealthCareTurnedAway> findByStudent(String schoolId, String studentId);
}
