package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Family;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Corrects the father's and mother's details recorded on a student at admission (names, phone numbers,
 * occupations). These are only a record on the student - the guardian account that signs in to the portal is
 * edited or changed separately (see ChangeStudentGuardianUseCase and the parent's own page).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateStudentFamilyUseCase {
    private final StudentRepository repo;
    private final GetStudentUseCase getStudentUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_FAMILY_EDITED")
    public StudentDTO execute(String schoolId, String studentId, Family family) {
        var student = repo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        var clean = new Family(trim(family.fatherName()), trim(family.motherName()),
                trim(family.fatherOccupation()), trim(family.motherOccupation()),
                trim(family.motherContact()), trim(family.fatherContact()));
        if (clean.fatherName() == null && clean.motherName() == null) {
            throw new RuleException("Enter at least the father's or the mother's name");
        }

        student.changeFamily(clean);
        repo.save(student);

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Parent details updated",
                student.getGivenNames() + " " + student.getFamilyName(), null, studentId);
        return getStudentUseCase.execute(studentId, schoolId, null);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }
}
