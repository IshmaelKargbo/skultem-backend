package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;

import lombok.Getter;

@Getter
public class Teacher extends AggregateRoot<String> {
    private String schoolId;
    private String phone;
    private String street;
    private String city;
    private String staffId;
    private Gender gender;
    private Title title;
    private User user;
    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public Teacher(String id, String schoolId, Title title, String phone, String street, String city, Gender gender, String staffId, User user,
            Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.user = user;
        this.phone = phone;
        this.city = city;
        this.street = street;
        this.gender = gender;
        this.staffId = staffId;
        this.title = title;
        this.status = status;
        touch(updatedAt);
    }

    public static Teacher create(String id, String schoolId, Title title, String phone,
            String street, String city, Gender gender, String staffId, User user) {
        Instant now = Instant.now();
        return new Teacher(id, schoolId, title, phone, street, city, gender, staffId, user, Status.ACTIVE, now,
                now);
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }
}
