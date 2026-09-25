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

    // Why/when a WITHDRAWN or EXPELLED student left - null for everyone else.
    private String exitReason;
    private LocalDate exitDate;
    private String exitNote;

    public enum Status {
        ACTIVE, GRADUATED, TRANSFERRED, SUSPENDED, DELETED,
        // Left the school before finishing - see leaveSchool. Kept on file (they can still owe fees).
        WITHDRAWN, EXPELLED
    }

    public enum EnrollmentType {
        NEW, TRANSFER, RE_ENROLLMENT,
        // Already attends the school but is only now being entered into the system - not a new
        // admission and not a transfer, so previous school / last class don't apply.
        EXISTING
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

    public void update(String admissionNumber, String givenNames, String familyName, Gender gender,
            LocalDate dateOfBirth, String nationality, String religion, String city, String street) {
        this.admissionNumber = admissionNumber;
        this.givenNames = givenNames;
        this.familyName = familyName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.religion = religion;
        this.city = city;
        this.street = street;
        touchNow();
    }

    public void setProfile(String profile) {
        photo = profile;
        touchNow();
    }

    public void assignHouse(House house) {
        this.house = house;
        touchNow();
    }

    public void softDelete() {
        this.status = Status.DELETED;
        touchNow();
    }

    public boolean hasLeft() {
        return status == Status.WITHDRAWN || status == Status.EXPELLED;
    }

    // type is WITHDRAWN (enrollment stopped) or EXPELLED.
    public void leaveSchool(Status type, String reason, LocalDate date, String note) {
        this.status = type;
        this.exitReason = reason;
        this.exitDate = date;
        this.exitNote = note;
        touchNow();
    }

    public void reinstate() {
        this.status = Status.ACTIVE;
        this.exitReason = null;
        this.exitDate = null;
        this.exitNote = null;
        touchNow();
    }

    // For the persistence mapper, which rebuilds the aggregate through the constructor.
    public void restoreExit(String reason, LocalDate date, String note) {
        this.exitReason = reason;
        this.exitDate = date;
        this.exitNote = note;
    }

    public void graduate() {
        this.status = Status.GRADUATED;
        touchNow();
    }

    public String getName() {
        return String.join(" ", givenNames, familyName);
    }

    private void touchNow() {
        touch(Instant.now());
    }
}
