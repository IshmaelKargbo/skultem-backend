package com.moriba.skultem.application.usecase;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.OwnerDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Owner;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.mail.MailService;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateSchoolUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateSchoolUseCase.class);

    private final SchoolRepository repo;
    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final ReferenceGeneratorUsecase rg;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final LogActivityUseCase logActivityUseCase;
    private final EnsurePlatformFeeSettingUseCase ensurePlatformFeeSettingUseCase;
    private final ProvisionAcademicCalendarForNewSchoolUseCase provisionAcademicCalendarUseCase;
    private final ProvisionStarterModulesForNewSchoolUseCase provisionStarterModulesUseCase;
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$!";
    private static final int PASSWORD_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    public SchoolDTO execute(String name, String domain, Address address, OwnerDTO ownerDto) {
        var cleanDomain = Generate.generateSubdomain(domain);

        if (repo.existsByDomain(cleanDomain)) {
            throw new AlreadyExistsException("domain already taken");
        }

        var hint = generatePassword();
        var id = rg.generate("SCHOOL", "SCL");
        var owner = new Owner(ownerDto.givenNames(), ownerDto.familyName(), ownerDto.email(), ownerDto.phone());
        var school = School.create(id, name, cleanDomain, address, owner);
        repo.save(school);

        // Best-effort - a new school missing its platform fee just gets caught by the startup
        // sweep instead (see BackfillPlatformFeesUseCase); it shouldn't block signup. Starts at the
        // default amount (150) with its "Platform Fee" category, so every student the school
        // admits is charged from its first term on - a SYSTEM_ADMIN can change the amount later.
        try {
            ensurePlatformFeeSettingUseCase.provisionForNewSchool(school.getId());
        } catch (Exception e) {
            log.warn("Could not set a default platform fee for new school {}: {}", school.getId(), e.getMessage());
        }

        // A new school starts with just the starter modules (see FeatureModule) and installs the rest
        // as it needs them. Best-effort like the rest: a missed one can be installed from the
        // Modules page.
        try {
            provisionStarterModulesUseCase.execute(school.getId(), null);
        } catch (Exception e) {
            log.warn("Could not install the starter modules for new school {}: {}", school.getId(), e.getMessage());
        }

        // After the platform fee on purpose: activating the current term seeds that fee's
        // structure, which needs the setting above to exist. Also best-effort - a school without
        // the ministry calendar just sets its year/terms up by hand, as before.
        try {
            provisionAcademicCalendarUseCase.execute(school.getId());
        } catch (Exception e) {
            log.warn("Could not set the academic calendar for new school {}: {}", school.getId(), e.getMessage());
        }

        logActivityUseCase.log(
                school.getId(),
                ActivityType.SCHOOL,
                "School created",
                school.getName(),
                null,
                school.getId());

        User user;
        if (userRepo.existsByEmail(ownerDto.email())) {
            user = userRepo.findByEmail(ownerDto.email()).orElseThrow();
        } else {
            var passwordHash = passwordEncoder.encode(hint);
            user = User.create(owner.givenNames(), owner.familyName(), owner.email(), passwordHash, hint);
            userRepo.save(user);
        }

        if (schoolUserRepo.existsBySchoolAndUserAndRole(school.getId(), user.getId(), Role.OWNER)) {
            throw new AlreadyExistsException("owner already exist with this email");
        }

        var schoolUser = SchoolUser.create(school.getId(), user, Role.OWNER);
        schoolUserRepo.save(schoolUser);

        sendWelcomeEmail(school, hint);

        return new SchoolDTO(school.getId(), school.getName(), school.getDomain(), school.getAddress(),
                school.getOwner(), school.getStatus(), school.getGradingScale(), school.getLogo(), school.getMotto(),
                school.getPrincipalName(), school.getPrincipalSignature(), school.getPrimaryColor(),
                school.getSecondaryColor(), school.getAttendanceThreshold(), school.getCreatedAt(),
                school.getUpdatedAt());
    }

    private void sendWelcomeEmail(School school, String password) {
        var subdomain = school.getDomain() + ".skultem.space";
        var link = "https://" + subdomain + "/login";
        mailService.sendWelcomeEmail(school.getOwner().email(), school.getOwner().givenNames(), password,
                subdomain, link, school.getName());
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
