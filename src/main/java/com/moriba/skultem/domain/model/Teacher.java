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

    // Free-text job title/role, e.g. "Mathematics Teacher", "Cleaner", "Security Guard", "Cook" -
    // this record doubles as the school's general staff record (payroll, salary structures and
    // attendance all key off it), so this is what tells a non-teaching staff member's role apart
    // from a classroom teacher's. Optional - null/blank is fine.
    private String designation;

    // Whether this record represents someone with actual classroom teaching duties - drives
    // whether the Subjects/Curriculum tabs show on their profile. False for staff added through
    // Add Staff, and for a User account (Admin/Accountant/Proprietor) opted into payroll - true
    // for everyone added through Add Teacher.
    private boolean teaching;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public Teacher(String id, String schoolId, Title title, String phone, String street, String city, Gender gender,
            String staffId, User user,
            Status status, String designation, boolean teaching, Instant createdAt, Instant updatedAt) {
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
        this.designation = designation;
        this.teaching = teaching;
        touch(updatedAt);
    }

    public static Teacher create(String id, String schoolId, Title title, String phone,
            String street, String city, Gender gender, String staffId, User user, String designation,
            boolean teaching) {
        Instant now = Instant.now();
        return new Teacher(id, schoolId, title, phone, street, city, gender, staffId, user, Status.ACTIVE,
                designation, teaching, now, now);
    }

    public Teacher update(Title title, String phone, String street,
            String city, Gender gender, String staffId, String designation) {
        if (title != null) {
            this.title = title;
        }
        if (phone != null) {
            this.phone = phone;
        }

        if (street != null) {
            this.street = street;
        }
        if (city != null) {
            this.city = city;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (staffId != null) {
            this.staffId = staffId;
        }
        if (designation != null) {
            this.designation = designation;
        }

        touch(Instant.now());
        return this;
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }

    public String getName() {
        return String.join(" ", title.toSentenceCase(), user.getName());
    }
}
