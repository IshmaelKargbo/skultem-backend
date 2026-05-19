package com.moriba.skultem.application.usecase;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.dto.ParentRequest;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.ParentMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateParentUseCase {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$!";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final ParentRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PARENT_CREATED")
    public ParentDTO execute(ParentRequest param) {
        var password = generatePassword();

        User user;
        if (userRepo.existsByEmail(param.email())) {
            user = userRepo.findByEmail(param.email()).orElseThrow();
        } else {
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(param.givenNames(), param.familyName(), param.email(), passwordHash, password);
            userRepo.save(user);
        }

        if (repo.existsByPhoneAndSchool(param.phone(), param.schoolId())) {
            throw new AlreadyExistsException("phone already exist in this school");
        }

        if (schoolUserRepo.existsBySchoolAndUserAndRole(param.schoolId(), user.getId(), Role.PARENT)) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var schoolUser = SchoolUser.create(param.schoolId(), user, Role.PARENT);
        schoolUserRepo.save(schoolUser);

        var domain = Parent.create(param.schoolId(), param.phone(), param.street(), param.city(), user);
        repo.save(domain);

        logActivityUseCase.log(
                param.schoolId(),
                ActivityType.PARENT,
                "New parent added",
                user.getGivenNames() + " " + user.getFamilyName(),
                null,
                domain.getId());

        return ParentMapper.toDTO(domain);
    }

    public Parent create(ParentRequest param) {
        var password = generatePassword();

        User user;
        if (userRepo.existsByEmail(param.email())) {
            user = userRepo.findByEmail(param.email()).orElseThrow();
        } else {
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(param.givenNames(), param.familyName(), param.email(), passwordHash, password);
            userRepo.save(user);
        }

        if (repo.existsByPhoneAndSchool(param.phone(), param.schoolId())) {
            throw new AlreadyExistsException("phone already exist in this school");
        }

        if (schoolUserRepo.existsBySchoolAndUserAndRole(param.schoolId(), user.getId(), Role.PARENT)) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var schoolUser = SchoolUser.create(param.schoolId(), user, Role.PARENT);
        schoolUserRepo.save(schoolUser);

        var domain = Parent.create(param.schoolId(), param.phone(), param.street(), param.city(), user);
        repo.save(domain);

        logActivityUseCase.log(
                param.schoolId(),
                ActivityType.PARENT,
                "New parent added",
                user.getGivenNames() + " " + user.getFamilyName(),
                null,
                domain.getId());

        return domain;
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
