package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Parent extends AggregateRoot<String> {
    private String schoolId;
    private String phone;
    private String fatherName;
    private String motherName;
    private String street;
    private String city;
    private User user;
    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public Parent(String id, String schoolId, String phone, String street, String city, String fatherName, String motherName, User user, Status status,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.user = user;
        this.phone = phone;
        this.city = city;
        this.street = street;
        this.status = status;
        this.fatherName = fatherName;
        this.motherName = motherName;
        touch(updatedAt);
    }

    public static Parent create(String id, String schoolId, String phone, String street, String city, String fatherName, String motherName, User user) {
        Instant now = Instant.now();
        return new Parent(id, schoolId, phone, street, city, fatherName, motherName, user, Status.ACTIVE, now, now);
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }
}
