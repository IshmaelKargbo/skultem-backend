package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.StudentParent;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Moves a student to a different guardian - e.g. one entered twice, or custody changing. The guardian
// link is kept in two places and both move together: Student.parent (whose portal lists the child) and
// the StudentParent row (the relationship, the guardian shown on the profile, and who is notified about
// attendance, behaviour and results). The old guardian keeps their account and any other children.
@Service
@Transactional
@RequiredArgsConstructor
public class ChangeStudentGuardianUseCase {

    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final StudentParentRepository studentParentRepo;
    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final GetStudentUseCase getStudentUseCase;
    private final LogActivityUseCase logActivityUseCase;

    // Where to move the student: an existing guardian (parentId), or any account already at this school
    // (userId) - their guardian profile is reused, or created from phone/street/city.
    public record Target(String parentId, String userId, String phone, String street, String city) {
    }

    @AuditLogAnnotation(action = "STUDENT_GUARDIAN_CHANGED")
    public StudentDTO execute(String schoolId, String studentId, Target target, String relationship) {
        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        var parent = resolveGuardian(schoolId, target);

        if (parent.getStatus() == Parent.Status.DELETED) {
            throw new RuleException("That guardian has been deleted - choose another one");
        }

        var links = studentParentRepo.findAllByStudentAndSchool(studentId, schoolId);
        String keptRelationship = links.stream().map(StudentParent::getRelationship)
                .filter(r -> r != null && !r.isBlank()).findFirst().orElse(null);
        // The column is NOT NULL - fall back to GUARDIAN (the add-student form's code) when there was never one recorded.
        String newRelationship = relationship != null && !relationship.isBlank() ? relationship.trim()
                : keptRelationship != null ? keptRelationship : "GUARDIAN";

        var old = student.getParent();
        boolean sameGuardian = old != null && old.getId().equals(parent.getId());

        // Lookups don't skip soft-deleted rows (and the profile expects exactly one), so the old link goes.
        links.forEach(studentParentRepo::delete);
        studentParentRepo.save(StudentParent.create(schoolId, newRelationship, parent, student));

        if (!sameGuardian) {
            student.changeGuardian(parent);
            studentRepo.save(student);
        }

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, sameGuardian ? "Guardian relationship updated" : "Guardian changed",
                student.getName() + " - " + (old != null ? old.getUser().getName() : "none") + " to "
                        + parent.getUser().getName() + " (" + newRelationship + ")",
                "from=" + (old != null ? old.getId() : "") + ";to=" + parent.getId(),
                student.getId());

        return getStudentUseCase.execute(studentId, schoolId, null);
    }

    private Parent resolveGuardian(String schoolId, Target target) {
        boolean byParent = target.parentId() != null && !target.parentId().isBlank();
        boolean byUser = target.userId() != null && !target.userId().isBlank();
        if (byParent == byUser) {
            throw new RuleException("Choose either an existing guardian or a user");
        }
        if (byParent) {
            return parentRepo.findByIdAndSchoolId(target.parentId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("Guardian not found"));
        }

        // Only someone who already belongs to this school - never an account from another school.
        if (schoolUserRepo.findAllByUser_IdAndSchoolId(target.userId(), schoolId).isEmpty()) {
            throw new NotFoundException("That user isn't part of this school");
        }
        var existing = parentRepo.findByUserIdAndSchoolId(target.userId(), schoolId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // No guardian profile here yet: give their existing account one (same login, guardian role added).
        String phone = target.phone() == null ? "" : target.phone().trim();
        if (phone.isEmpty()) {
            throw new RuleException("A phone number is needed to make this user a guardian");
        }
        if (parentRepo.existsByPhoneAndSchool(phone, schoolId)) {
            throw new AlreadyExistsException("Another guardian already uses this phone number");
        }
        var user = userRepo.findById(target.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!schoolUserRepo.existsBySchoolAndUserAndRole(schoolId, user.getId(), Role.PARENT)) {
            schoolUserRepo.save(SchoolUser.create(schoolId, user, Role.PARENT));
        }
        var parent = Parent.create(schoolId, phone, blankToEmpty(target.street()), blankToEmpty(target.city()), user);
        parentRepo.save(parent);
        logActivityUseCase.log(schoolId, ActivityType.PARENT, "New parent added",
                user.getGivenNames() + " " + user.getFamilyName(), "fromExistingUser=" + user.getId(), parent.getId());
        return parent;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
