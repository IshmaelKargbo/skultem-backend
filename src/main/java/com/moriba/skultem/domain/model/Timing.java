package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;

// A named, reusable school-day schedule. A school can have several of these (e.g. "Default",
// "Primary", "JSS/SSS") - CreatePeriodUseCase resolves which one applies to a given class session
// via its Level, falling back to whichever Timing has isDefault=true when a Level has no template
// of its own assigned (see TimingLevel).
@Getter
@EqualsAndHashCode(callSuper = true)
public class Timing extends AggregateRoot<String> {

    private final String schoolId;
    private String name;
    private boolean isDefault;
    private LocalTime startTime;
    private LocalTime endTime;
    private int periodDuration;
    private int breakDuration;
    private int lunchDuration;

    public Timing(String id, String schoolId, String name, boolean isDefault, LocalTime startTime, LocalTime endTime,
                  int periodDuration, int breakDuration, int lunchDuration, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.isDefault = isDefault;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodDuration = periodDuration;
        this.breakDuration = breakDuration;
        this.lunchDuration = lunchDuration;
        touch(updatedAt);
    }

    public static Timing create(String id, String schoolId, String name, boolean isDefault, LocalTime startTime,
                                 LocalTime endTime, int periodDuration, int breakDuration, int lunchDuration) {
        Instant now = Instant.now();
        return new Timing(id, schoolId, name, isDefault, startTime, endTime, periodDuration, breakDuration,
                lunchDuration, now, now);
    }

    public void updateSchedule(String name, LocalTime startTime, LocalTime endTime, int periodDuration,
                                int breakDuration, int lunchDuration) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodDuration = periodDuration;
        this.breakDuration = breakDuration;
        this.lunchDuration = lunchDuration;
        touch();
    }

    public void markAsDefault() {
        this.isDefault = true;
        touch();
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
