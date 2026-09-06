package com.moriba.skultem.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The only path onto {@link Role#SYSTEM_ADMIN} - CreateUserUseCase and AssignRoleUseCase both
 * refuse it, since those are reachable by any school's own ADMIN/OWNER/PROPRIETOR. This endpoint
 * is reachable pre-auth (see SecurityConfig), so it's gated two ways instead of {@code @PreAuthorize}:
 * a caller-supplied token checked against {@code system.admin.bootstrap-token} (unset by default -
 * fails closed, not open), and a check that no SYSTEM_ADMIN already exists anywhere in the system.
 * The latter makes this self-disabling after first use - once bootstrapped, further SYSTEM_ADMIN
 * accounts are created the ordinary way (an existing SYSTEM_ADMIN using CreateUserUseCase, which
 * permits it - see there) rather than through this endpoint again.
 * <p>
 * {@code domain} is optional. {@code school_users.school_id} is NOT NULL at the DB level, so the
 * new SchoolUser row still needs some school to point at, but which one doesn't matter - see
 * PermissionService.isSystemAdmin(), which grants full access off the role alone and never
 * consults the anchor school. Callers who don't care can omit domain and let this pick any
 * existing school; only pass one to anchor onto a specific school on purpose.
 * <p>
 * Deliberately not {@code @AuditLogAnnotation} - that aspect logs every argument verbatim
 * (see AuditAspect.generateDetails), which would put the bootstrap token and the new admin's
 * plaintext password straight into the audit log. LogActivityUseCase below records the outcome
 * without either.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BootstrapSystemAdminUseCase {

    private final UserRepository userRepo;
    private final SchoolRepository schoolRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final LogActivityUseCase logActivityUseCase;

    @Value("${system.admin.bootstrap-token:}")
    private String configuredToken;

    public UserDTO execute(String token, String domain, String email, String password, String givenNames,
            String familyName) {
        if (configuredToken == null || configuredToken.isBlank() || !constantTimeEquals(configuredToken, token)) {
            throw new AccessDeniedException("Invalid or disabled bootstrap token");
        }

        if (schoolUserRepo.existsByRole(Role.SYSTEM_ADMIN)) {
            throw new RuleException("A system admin already exists - use an existing admin account to create more");
        }

        var school = resolveAnchorSchool(domain);

        User user;
        if (userRepo.existsByEmail(email)) {
            // Reusing an existing account (e.g. an OWNER promoting themselves to also run the
            // platform) - only the SchoolUser row below is new, their existing credentials are
            // left untouched.
            user = userRepo.findByEmail(email).orElseThrow();
        } else {
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(givenNames, familyName, email, passwordHash, "");
            // User.create always starts a fresh account at RESET_PASSWORD (see CreateUserUseCase,
            // which relies on that to force a temp/generated password to be changed on first
            // login). Here the caller already chose their own password directly, so there's
            // nothing to force a reset over - flip straight to ACTIVE with that same hash so the
            // account can log in immediately.
            user.resetPassword(passwordHash);
            userRepo.save(user);
        }

        var schoolUser = SchoolUser.create(school.getId(), user, Role.SYSTEM_ADMIN);
        schoolUserRepo.save(schoolUser);

        logActivityUseCase.log(
                school.getId(),
                ActivityType.USER,
                "System admin bootstrapped",
                user.getGivenNames() + " " + user.getFamilyName(),
                null,
                user.getId());

        return UserMapper.toDTO(user, List.of(Role.SYSTEM_ADMIN));
    }

    private School resolveAnchorSchool(String domain) {
        if (domain != null && !domain.isBlank()) {
            return schoolRepo.findByDomain(domain)
                    .orElseThrow(() -> new NotFoundException("School not found"));
        }

        return schoolRepo.findAll(PageRequest.of(0, 1)).stream().findFirst()
                .orElseThrow(() -> new RuleException(
                        "No school exists yet - create one first (POST /api/v1/school), or pass domain to anchor onto a specific one"));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
