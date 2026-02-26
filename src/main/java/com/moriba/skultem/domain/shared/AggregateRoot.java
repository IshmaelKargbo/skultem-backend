package com.moriba.skultem.domain.shared;

import java.time.Instant;

import lombok.Getter;

@Getter
public abstract class AggregateRoot<T> {

    protected final T id;
    protected final Instant createdAt;
    protected Instant updatedAt;

    protected AggregateRoot(T id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    protected void touch(Instant now) {
        this.updatedAt = now;
    }
}
