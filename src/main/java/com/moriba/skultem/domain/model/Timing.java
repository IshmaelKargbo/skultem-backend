package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Timing extends AggregateRoot<String> {

    private final String schoolId;
    private LocalTime startTime;
    private LocalTime endTime;
    private int periodDuration;
    private int breakDuration;
    private int lunchDuration;

    public Timing(String id, String schoolId, LocalTime startTime, LocalTime endTime, int periodDuration, int breakDuration,
                  int lunchDuration, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodDuration = periodDuration;
        this.breakDuration = breakDuration;
        this.lunchDuration = lunchDuration;
        touch(updatedAt);
    }

    public static Timing create(String id, String schoolId, LocalTime startTime, LocalTime endTime, int periodDuration, int breakDuration, int lunchDuration) {
        Instant now = Instant.now();
        return new Timing(id, schoolId, startTime, endTime, periodDuration, breakDuration, lunchDuration, now, now);
    }

    public void updateSchedule(LocalTime startTime, LocalTime endTime, int periodDuration, int breakDuration, int lunchDuration) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodDuration = periodDuration;
        this.breakDuration = breakDuration;
        this.lunchDuration = lunchDuration;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}