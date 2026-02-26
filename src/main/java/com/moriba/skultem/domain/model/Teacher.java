package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Teacher extends AggregateRoot<String> {
    private String schoolId;
    private String phone;
    private Address address;
    private String staffId;
    private User user;
    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public Teacher(String id, String schoolId, String phone, Address address, String staffId, User user,
            Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.user = user;
        this.phone = phone;
        this.address = address;
        this.staffId = staffId;
        this.status = status;
        touch(updatedAt);
    }

    public static Teacher create(String id, String schoolId, String phone,
            Address address, String staffId, User user) {
        Instant now = Instant.now();
        return new Teacher(id, schoolId, phone, address, staffId, user, Status.ACTIVE, now,
                now);
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }
}
