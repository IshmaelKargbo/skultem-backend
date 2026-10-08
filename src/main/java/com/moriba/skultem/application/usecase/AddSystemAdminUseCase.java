package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Lets an existing SYSTEM_ADMIN add another one - the follow-up to BootstrapSystemAdminUseCase,
 * which only works while none exist. Like bootstrap, the SchoolUser row has a null school_id (a
 * system admin isn't tied to any school) and the caller picks the initial password directly, so
 * the account can log in at once. Deliberately not {@code @AuditLogAnnotation}: that aspect logs
 * every argument verbatim, which would put the plaintext password in the audit log.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AddSystemAdminUseCase {

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final PasswordEncoder passwordEncoder;

    public UserDTO execute(String email, String password, String givenNames, String familyName) {
        User user;
        if (userRepo.existsByEmail(email)) {
            // An existing account (e.g. a school owner) is promoted; its credentials are left alone.
            user = userRepo.findByEmail(email).orElseThrow();
            boolean alreadyAdmin = schoolUserRepo.findAllByUser_Id(user.getId()).stream()
                    .anyMatch(su -> su.getRole() == Role.SYSTEM_ADMIN);
            if (alreadyAdmin) {
                throw new AlreadyExistsException("This user is already a system admin");
            }
        } else {
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(givenNames, familyName, email, passwordHash, "");
            user.resetPassword(passwordHash);
            userRepo.save(user);
        }

        schoolUserRepo.save(SchoolUser.create(null, user, Role.SYSTEM_ADMIN));

        return UserMapper.toDTO(user, List.of(Role.SYSTEM_ADMIN));
    }
}
