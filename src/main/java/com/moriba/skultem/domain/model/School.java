package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.model.vo.Owner;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class School extends AggregateRoot<String> {

    private String name;
    private Address address;
    private String domain;
    private Owner owner;
    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public School(String id, String name, String domain, Address address, Owner owner, Status status, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.address = address;
        this.status = status;
        this.owner = owner;
        this.domain = domain;
        touch(updatedAt);
    }

    public static School create(String id, String name, String domain, Address address, Owner owner) {
        Instant now = Instant.now();
        return new School(id, name, domain, address, owner, Status.ACTIVE, now, now);
    }

    public void update(String name, String domain, Address address) {
        this.address = address;
        this.name = name;
        this.domain = domain;
    }

    public void setStatus(Status status) {
        this.status = status;
        touch(Instant.now());
    }
}
