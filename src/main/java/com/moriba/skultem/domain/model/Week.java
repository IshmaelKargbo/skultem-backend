package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Week extends AggregateRoot<String> {

    private final String schoolId;
    private final int week;
    private String topic;
    private List<String> objectives;
    private String subTopic;
    private SchemeOfWork scheme;
    private State state;

    public enum State {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    public Week(String id, String schoolId, int week, String topic, String subTopic, List<String> objectives,
            SchemeOfWork scheme, State state, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.week = week;
        this.topic = topic;
        this.subTopic = subTopic;
        this.objectives = objectives;
        this.scheme = scheme;
        this.state = state;
        touch(updatedAt);
    }

    public static Week create(String id, String schoolId, int week, String topic, String subTopic, List<String> objectives,
            SchemeOfWork scheme) {
        Instant now = Instant.now();
        return new Week(id, schoolId, week, topic, subTopic, objectives, scheme, State.NOT_STARTED, now, now);
    }

    public void setState(State state) {
        this.state = state;
        touch(Instant.now());
    }

    // Rolls up a scheme's week states into a single overall progress state:
    // no weeks yet, or none started -> NOT_STARTED; every week completed -> COMPLETED;
    // anything else (a mix, or some started) -> IN_PROGRESS.
    public static State deriveProgress(List<State> weekStates) {
        if (weekStates.isEmpty())
            return State.NOT_STARTED;

        if (weekStates.stream().allMatch(s -> s == State.COMPLETED))
            return State.COMPLETED;

        if (weekStates.stream().anyMatch(s -> s != State.NOT_STARTED))
            return State.IN_PROGRESS;

        return State.NOT_STARTED;
    }
}
