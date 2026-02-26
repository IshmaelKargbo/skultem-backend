package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Student extends AggregateRoot<String> {
    private String schoolId;
    private String admissionNumber;
    private String givenNames;
    private String familyName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Status status;

    public enum Gender {
        MALE,
        FEMALE
    }

    public enum Status {
        ACTIVE, GRADUATED, TRANSFERRED, SUSPENDED, DELETED
    }

    public Student(String id, String schoolId, String admissionNumber, String givenNames, String familyName,
            Gender gender,
            LocalDate dateOfBirth, Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.admissionNumber = admissionNumber;
        this.givenNames = givenNames;
        this.familyName = familyName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.status = status;
        touch(updatedAt);
    }

    public static Student create(String id, String schoolId, String admissionNumber, String givenNames,
            String familyName, Gender gender,
            LocalDate dateOfBirth) {
        Instant now = Instant.now();
        return new Student(id, schoolId, admissionNumber, givenNames, familyName, gender, dateOfBirth, Status.ACTIVE,
                now, now);
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }

    public String getName() {
        return String.join(" ", givenNames, familyName);
    }
}
