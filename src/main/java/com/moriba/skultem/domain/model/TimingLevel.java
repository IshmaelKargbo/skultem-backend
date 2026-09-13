package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Level;

import lombok.Getter;

// Which Timing template a Level (PRIMARY/JSS/SSS) uses. timing is intentionally not unique here -
// the same template can be assigned to more than one level (e.g. one shared template for both
// JSS and SSS). At most one row per (schoolId, level) - see TimingLevelRepository.
@Getter
public class TimingLevel extends AggregateRoot<String> {

    private String schoolId;
    private Timing timing;
    private Level level;

    public TimingLevel(String id, String schoolId, Timing timing, Level level, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.timing = timing;
        this.level = level;
        touch(updatedAt);
    }

    public static TimingLevel create(String id, String schoolId, Timing timing, Level level) {
        Instant now = Instant.now();
        return new TimingLevel(id, schoolId, timing, level, now, now);
    }

    public void reassign(Timing timing) {
        this.timing = timing;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
