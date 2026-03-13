package com.moriba.skultem.application.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;
    private final ReferenceGeneratorUsecase rg;
    private final PasswordEncoder passwordEncoder;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action="USER_CREATED")
    public UserDTO execute(String school, String givenNames, String familyName, String email, String password, String role) {
        User user;
        if (repo.existsByEmail(email)) {
            user = repo.findByEmail(email).orElseThrow();
        } else {
            var id = rg.generate("USER", "USR");
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(id, givenNames, familyName, email, passwordHash, "");
            repo.save(user);
        }

        if (schoolUserRepo.existsBySchoolAndUser(school, user.getId())) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var roleEnum = Role.valueOf(role);
        var schoolUserId = rg.generate("SCHOOL_USER", "SCU");
        var schoolUser = SchoolUser.create(schoolUserId, school, user, roleEnum);
        schoolUserRepo.save(schoolUser);

        logActivityUseCase.log(
                school,
                ActivityType.USER,
                "New user added",
                user.getGivenNames() + " " + user.getFamilyName() + " (" + roleEnum.name() + ")",
                null,
                user.getId());

        return UserMapper.toDTO(user, schoolUser);
    }
}
