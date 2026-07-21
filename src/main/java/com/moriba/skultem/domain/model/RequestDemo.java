package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class RequestDemo extends AggregateRoot<UUID> {

    private final String name;
    private final String email;
    private final String school;
    private final String city;
    private final String address;
    private final String phone;
    private final String preferred;
    private final String priority;
    private final String message;

    public RequestDemo(UUID id, String name, String email, String school, String phone, String city, String address, String preferred, String priority, String message, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.email = email;
        this.school = school;
        this.phone = phone;
        this.preferred = preferred;
        this.priority = priority;
        this.message = message;
        this.address = address;
        this.city = city;
        touch(updatedAt);
    }

    public static RequestDemo create(String name, String email, String school, String phone, String city, String address, String preferred, String priority, String message) {
        Instant now = Instant.now();
        var id = UUID.randomUUID();
        return new RequestDemo(id, name, email, school, phone, city, address, preferred, priority, message, now, now);
    }
}
