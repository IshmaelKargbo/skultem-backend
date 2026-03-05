package com.moriba.skultem.application.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.OwnerDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.model.vo.Owner;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateSchoolUseCase {

    private final SchoolRepository repo;
    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final ReferenceGeneratorUsecase rg;
    private final PasswordEncoder passwordEncoder;

    public SchoolDTO execute(String name, String domain, Address address, OwnerDTO ownerDto) {
        var cleanDomain = Generate.generateSubdomain(domain);

        if (repo.existsByDomain(cleanDomain)) {
            throw new AlreadyExistsException("domain already taken");
        }

        var id = rg.generate("SCHOOL", "SCL");
        var owner = new Owner(ownerDto.givenNames(), ownerDto.familyName(), ownerDto.email(), ownerDto.phone());
        var school = School.create(id, name, cleanDomain, address, owner);
        repo.save(school);

        User user;
        if (userRepo.existsByEmail(ownerDto.email())) {
            user = userRepo.findByEmail(ownerDto.email()).orElseThrow();
        } else {
            var userId = rg.generate("USER", "USR");
            var password = passwordEncoder.encode(ownerDto.password());
            user = User.create(userId, owner.givenNames(), owner.familyName(), owner.email(), password, "");
            userRepo.save(user);
        }

        if (schoolUserRepo.existsBySchoolAndUser(school.getId(), user.getId())) {
            throw new AlreadyExistsException("owner already exist with this email");
        }

        var schoolUserId = rg.generate("SCHOOL_USER", "SCU");
        var schoolUser = SchoolUser.create(schoolUserId, school.getId(), user, Role.SCHOOL_ADMIN);
        schoolUserRepo.save(schoolUser);

        return new SchoolDTO(school.getId(), school.getName(), school.getDomain(), school.getAddress(),
                school.getOwner(), school.getStatus(), school.getGradingScale(), school.getCreatedAt(),
                school.getUpdatedAt());
    }
}
