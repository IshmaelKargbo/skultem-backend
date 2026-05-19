package com.moriba.skultem.application.usecase;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.mail.MailService;
import com.moriba.skultem.infrastructure.mail.MailService.WelcomeUserPayload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$!";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;
    private final SchoolRepository schoolRepo;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "USER_CREATED")
    public UserDTO execute(String schoolId, String givenNames, String familyName, String email, String role) {
        User user;

        var password = generatePassword();

        if (repo.existsByEmail(email)) {
            user = repo.findByEmail(email).orElseThrow();
        } else {
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(givenNames, familyName, email, passwordHash, password);
            repo.save(user);
        }

        if (schoolUserRepo.existsBySchoolAndUserAndRole(schoolId, user.getId(), Role.valueOf(role))) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));

        var roleEnum = Role.valueOf(role);
        var schoolUser = SchoolUser.create(schoolId, user, roleEnum);

        List<Role> roles = schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), schoolId).stream()
                .map(e -> e.getRole())
                .toList();
        schoolUserRepo.save(schoolUser);

        logActivityUseCase.log(
                schoolId,
                ActivityType.USER,
                "New user added",
                user.getGivenNames() + " " + user.getFamilyName() + " (" + roleEnum.name() + ")",
                null,
                user.getId());
        sendAssignEmail(school, user, role, password);

        return UserMapper.toDTO(user, roles);
    }

    private String generatePassword() {
        StringBuilder value = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(PASSWORD_CHARS.length());
            value.append(PASSWORD_CHARS.charAt(index));
        }
        return value.toString();
    }

    private void sendAssignEmail(School school, User user, String role, String password) {
        var subdomain = school.getDomain() + ".skultem.space";
        var link = "https://" + subdomain + "/login";
        var param = new WelcomeUserPayload(user.getEmail(), user.getName(), password, link, school.getName(), role,
                subdomain);
        mailService.sendWelcomeUserEmail(param);
    }
}
