package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Gender;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Covers the student's own personal-information fields plus admission number - the same set as
// step 1 of enrollment (CreateStudentUseCase/personal.vue), plus the identifier itself for
// correcting a typo made at enrollment time. Admission date, enrollment type, class and parent
// linking are deliberately out of scope here: those go through their own dedicated flows (class
// promotion/transfer, house assignment, family management) rather than being folded into a
// single edit form.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateStudentUseCase {

    private final StudentRepository repo;
    private final SchoolRepository schoolRepo;
    private final GetStudentUseCase getStudentUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_EDITED")
    public StudentDTO execute(String schoolId, String studentId, String admissionNumber, String givenNames,
            String familyName, Gender gender, LocalDate dateOfBirth, String nationality, String religion,
            String city, String street) {

        schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));

        Student student = repo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        String normalizedAdmissionNumber = admissionNumber.trim().toUpperCase();
        if (repo.existsByAdmissionNumberAndSchoolIdAndIdNot(normalizedAdmissionNumber, schoolId, studentId)) {
            throw new AlreadyExistsException("Admission number already exists in this school");
        }

        String oldAdmissionNumber = student.getAdmissionNumber();
        String oldName = student.getName();
        String oldGender = student.getGender() != null ? student.getGender().name() : null;
        String oldDateOfBirth = student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : null;
        String oldNationality = student.getNationality();
        String oldReligion = student.getReligion();
        String oldCity = student.getCity();
        String oldStreet = student.getStreet();

        student.update(normalizedAdmissionNumber, givenNames, familyName, gender, dateOfBirth, nationality, religion,
                city, street);
        repo.save(student);

        String meta = buildMeta(oldAdmissionNumber, normalizedAdmissionNumber, oldName, student.getName(), oldGender,
                gender != null ? gender.name() : null, oldDateOfBirth,
                dateOfBirth != null ? dateOfBirth.toString() : null, oldNationality, nationality, oldReligion,
                religion, oldCity, city, oldStreet, street);

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Student updated",
                student.getName() + " - " + student.getAdmissionNumber(), meta, student.getId());

        return getStudentUseCase.execute(studentId, schoolId, null);
    }

    private String buildMeta(String oldAdmissionNumber, String newAdmissionNumber, String oldName, String newName,
            String oldGender, String newGender, String oldDateOfBirth, String newDateOfBirth,
            String oldNationality, String newNationality, String oldReligion, String newReligion, String oldCity,
            String newCity, String oldStreet, String newStreet) {
        StringBuilder meta = new StringBuilder();

        append(meta, "admissionNumber", oldAdmissionNumber, newAdmissionNumber);
        append(meta, "name", oldName, newName);
        append(meta, "gender", oldGender, newGender);
        append(meta, "dateOfBirth", oldDateOfBirth, newDateOfBirth);
        append(meta, "nationality", oldNationality, newNationality);
        append(meta, "religion", oldReligion, newReligion);
        append(meta, "city", oldCity, newCity);
        append(meta, "street", oldStreet, newStreet);

        return meta.toString();
    }

    private void append(StringBuilder meta, String field, String oldVal, String newVal) {
        if (oldVal == null && newVal == null)
            return;
        if (oldVal != null && oldVal.equals(newVal))
            return;

        if (meta.length() > 0)
            meta.append(", ");

        meta.append(field).append(": ").append(oldVal).append(" → ").append(newVal);
    }
}
