package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ParentMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.infrastructure.mail.MailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A parent enrolled without an email (not every parent has one) gets one added here later by an
// admin - this is what actually grants them portal access, since login is keyed on email and they
// had no way to authenticate before this. Fires the same welcome email a new parent gets at
// enrollment time (their generated password is still sitting in User.hint, untouched until now).
@Service
@Transactional
@RequiredArgsConstructor
public class AddParentEmailUseCase {

    private final ParentRepository parentRepo;
    private final UserRepository userRepo;
    private final StudentRepository studentRepo;
    private final SchoolRepository schoolRepo;
    private final MailService mailService;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PARENT_EMAIL_ADDED")
    public ParentDTO execute(String schoolId, String parentId, String email) {
        Parent parent = parentRepo.findByIdAndSchoolId(parentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Parent not found"));

        var user = parent.getUser();
        String normalized = email.trim();

        if (userRepo.existsByEmail(normalized)) {
            throw new AlreadyExistsException("Email already in use by another account");
        }

        user.updateEmail(normalized);
        userRepo.save(user);

        School school = schoolRepo.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));

        sendWelcomeEmail(school, parent, normalized);

        logActivityUseCase.log(
                schoolId,
                ActivityType.PARENT,
                "Parent email added",
                user.getGivenNames() + " " + user.getFamilyName(),
                null,
                parent.getId());

        return ParentMapper.toDTO(parent);
    }

    private void sendWelcomeEmail(School school, Parent parent, String email) {
        var students = studentRepo.findByParentAndSchoolId(parent.getId(), school.getId(), Pageable.unpaged())
                .getContent();

        // No students yet is possible if the email is added independently of any enrollment -
        // still worth notifying them since access was just granted.
        String studentName = students.isEmpty() ? "your child" : joinNames(students);
        String className = students.size() == 1 ? students.get(0).getSession().getName() : "N/A";

        var subdomain = school.getDomain() + ".skultem.space";
        var link = "https://" + subdomain + "/login";
        var password = parent.getUser().getHint();

        mailService.sendParentEmail(email, studentName, className, password, link, school.getName(), subdomain);
    }

    private String joinNames(List<Student> students) {
        var names = students.stream().map(Student::getName).distinct().toList();
        if (names.size() <= 1) {
            return names.isEmpty() ? "your child" : names.get(0);
        }
        return names.get(0) + " (+" + (names.size() - 1) + ")";
    }
}
