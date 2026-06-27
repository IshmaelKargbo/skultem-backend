package com.moriba.skultem.domain.model;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@EqualsAndHashCode(callSuper = true)
public class SchemeOfWork extends AggregateRoot<String> {

    private final String schoolId;
    private final Subject subject;
    private final ClassSession session;
    private Term term;
    private long weeks;
    private State state;

    public enum State {
        DRAFT, PUBLISH
    }

    public SchemeOfWork(String id, String schoolId, Subject subject, ClassSession session, Term term, long weeks, State state, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.subject = subject;
        this.session = session;
        this.term = term;
        this.weeks = weeks;
        this.state = state;
        touch(updatedAt);
    }

    public static SchemeOfWork create(String id, String schoolId, Subject subject, ClassSession session, Term term, long weeks) {
        Instant now = Instant.now();
        return new SchemeOfWork(id, schoolId, subject, session, term, weeks, State.DRAFT, now, now);
    }

    public void setState(State state) {
        this.state = state;
        touch(Instant.now());
    }
}
