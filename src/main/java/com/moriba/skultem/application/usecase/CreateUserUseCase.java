package com.moriba.skultem.application.usecase;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$!";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;
    private final ReferenceGeneratorUsecase rg;
    private final PasswordEncoder passwordEncoder;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "USER_CREATED")
    public UserDTO execute(String school, String givenNames, String familyName, String email, String role) {
        User user;
        if (repo.existsByEmail(email)) {
            user = repo.findByEmail(email).orElseThrow();
        } else {
            var password = generatePassword();
            var id = rg.generate("USER", "USR");
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(id, givenNames, familyName, email, passwordHash, password);
            repo.save(user);
        }

        if (schoolUserRepo.existsBySchoolAndUserAndRole(school, user.getId(), Role.valueOf(role))) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var roleEnum = Role.valueOf(role);
        var schoolUserId = rg.generate("SCHOOL_USER", "SCU");
        var schoolUser = SchoolUser.create(schoolUserId, school, user, roleEnum);

        List<Role> roles = schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), school).stream()
                .map(e -> e.getRole())
                .toList();
        schoolUserRepo.save(schoolUser);

        logActivityUseCase.log(
                school,
                ActivityType.USER,
                "New user added",
                user.getGivenNames() + " " + user.getFamilyName() + " (" + roleEnum.name() + ")",
                null,
                user.getId());

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
}
