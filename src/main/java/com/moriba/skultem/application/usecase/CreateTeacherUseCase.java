package com.moriba.skultem.application.usecase;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.domain.vo.Title;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateTeacherUseCase {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$!";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final TeacherRepository repo;
    private final ClassSessionRepository classSessionRepo;
    private final AssignTeacherToClassUseCase assignTeacherToClassUseCase;
    private final ReferenceGeneratorUsecase rg;
    private final PasswordEncoder passwordEncoder;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_CREATED")
    public TeacherDTO execute(String schoolId, Title title, String givenNames, String familyName, Gender gender,
            String staffId, String email,
            String phone, String street, String city, String classMaster) {
        var password = generatePassword();

        User user;
        if (userRepo.existsByEmail(email)) {
            user = userRepo.findByEmail(email).orElseThrow();
        } else {
            var id = rg.generate("USER", "USR");
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(id, givenNames, familyName, email, passwordHash, password);
            userRepo.save(user);
        }

        if (repo.existsByStaffIdAndSchool(schoolId, staffId)) {
            throw new AlreadyExistsException("staffId already exist in this school");
        }

        if (repo.existsByPhoneAndSchool(schoolId, phone)) {
            throw new AlreadyExistsException("phone already exist in this school");
        }

        if (schoolUserRepo.existsBySchoolAndUserAndRole(schoolId, user.getId(), Role.TEACHER)) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var schoolUserId = rg.generate("SCHOOL_USER", "SCU");
        var schoolUser = SchoolUser.create(schoolUserId, schoolId, user, Role.TEACHER);
        schoolUserRepo.save(schoolUser);

        var teacherId = rg.generate("TEACHER", "THR");
        var teacher = Teacher.create(teacherId, schoolId, title, phone, street, city, gender, staffId, user);
        repo.save(teacher);

        if (!classMaster.isEmpty() && !classMaster.isBlank()) {
            var clazz = classSessionRepo.findByIdAndSchoolId(classMaster, schoolId)
                    .orElseThrow(() -> new NotFoundException("class not found"));
            String streamId = "";

            if (clazz.getStream() != null) {
                streamId = clazz.getStream().getId();
            }

            assignTeacherToClassUseCase.execute(schoolId, clazz.getClazz().getId(), teacherId,
                    clazz.getSection().getId(), streamId);
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.TEACHER,
                "New teacher added",
                user.getGivenNames() + " " + user.getFamilyName() + " - " + teacher.getStaffId(),
                null,
                teacher.getId());

        return TeacherMapper.toDTO(teacher);
    }

    private String generatePassword() {
        StringBuilder value = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(PASSWORD_CHARS.length());
            value.append(PASSWORD_CHARS.charAt(index));
        }
        return value.toString();
    }
}
