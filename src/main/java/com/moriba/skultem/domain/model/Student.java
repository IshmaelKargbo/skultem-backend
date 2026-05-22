package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Family;
import com.moriba.skultem.domain.vo.Gender;

import lombok.Getter;

@Getter
public class Student extends AggregateRoot<String> {
    private String schoolId;
    private String admissionNumber;
    private LocalDate admissionDate;
    private EnrollmentType enrollmentType;
    private String previousSchool;
    private String givenNames;
    private String familyName;
    private String nationality;
    private String religion;
    private String photo;
    private Gender gender;
    private String city;
    private String street;
    private Family family;
    private ClassSession session;
    private String lastClass;
    private LocalDate dateOfBirth;
    private Parent parent;
    private House house;
    private Status status;

    public enum Status {
        ACTIVE, GRADUATED, TRANSFERRED, SUSPENDED, DELETED
    }

    public enum EnrollmentType {
        NEW, TRANSFER, RE_ENROLLMENT
    }

    public Student(String id, String schoolId, String photo, String admissionNumber, LocalDate admissionDate,
            String givenNames, String familyName, Family family, ClassSession session, String lastClass, Gender gender,
            Parent parent, LocalDate dateOfBirth, EnrollmentType enrollmentType, String previousSchool, House house,
            String nationality, String religion, String city, String street, Status status, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.admissionNumber = admissionNumber;
        this.admissionDate = admissionDate;
        this.givenNames = givenNames;
        this.familyName = familyName;
        this.dateOfBirth = dateOfBirth;
        this.family = family;
        this.session = session;
        this.parent = parent;
        this.gender = gender;
        this.photo = photo;
        this.previousSchool = previousSchool;
        this.enrollmentType = enrollmentType;
        this.lastClass = lastClass;
        this.status = status;
        this.house = house;
        this.nationality = nationality;
        this.religion = religion;
        this.nationality = nationality;
        this.city = city;
        this.street = street;
        touch(updatedAt);
    }

    public static Student create(String schoolId, String photo, String admissionNumber,
            LocalDate admissionDate, String givenNames, String familyName, Family family, ClassSession session,
            String lastClass, Gender gender, Parent parent, LocalDate dateOfBirth, EnrollmentType enrollmentType,
            String previousSchool, House house, String nationality, String religion, String city, String street) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        return new Student(id, schoolId, photo, admissionNumber, admissionDate, givenNames, familyName, family, session,
                lastClass, gender, parent, dateOfBirth, enrollmentType, previousSchool, house, nationality, religion,
                city, street, Status.ACTIVE, now, now);
    }

    public void setProfile(String profile) {
        photo = profile;
        touch(Instant.now());
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touch(Instant.now());
    }

    public String getName() {
        return String.join(" ", givenNames, familyName);
    }
}
