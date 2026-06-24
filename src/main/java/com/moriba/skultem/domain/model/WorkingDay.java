package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@EqualsAndHashCode(callSuper = true)
public class WorkingDay extends AggregateRoot<String> {

    private final String schoolId;
    private final Timing timing;
    private boolean state;
    private final Day day;

    public enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    public WorkingDay(String id, String schoolId, Timing timing, Day day, boolean state, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.timing = timing;
        this.day = day;
        this.state = state;
        touch(updatedAt);
    }

    public static WorkingDay create(String id, String schoolId, Timing timing, Day day, boolean state) {
        Instant now = Instant.now();
        return new WorkingDay(id, schoolId, timing, day, state, now, now);
    }

    public void setState(boolean state) {
        this.state = state;
        touch(Instant.now());
    }
}
