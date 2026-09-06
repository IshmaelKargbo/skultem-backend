package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserSchoolMembershipDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * System-admin-only: activates/deactivates one user's membership at one school - e.g. reinstating
 * a locked-out owner, or shutting off a compromised account's access to a single school - without
 * touching that same person's membership at any other school. Distinct from
 * SetSchoolStatusUseCase (which (de)activates the whole school) and from User.Status (the account
 * itself, shared across every school it belongs to).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SetSchoolUserStatusUseCase {
    private final SchoolUserRepository schoolUserRepo;
    private final SchoolRepository schoolRepo;

    public UserSchoolMembershipDTO execute(String schoolId, String userId, String status) {
        var schoolUser = schoolUserRepo.findBySchoolAndUser(schoolId, userId)
                .orElseThrow(() -> new NotFoundException("membership not found"));

        schoolUser.setStatus(SchoolUser.Status.valueOf(status));
        schoolUserRepo.save(schoolUser);

        var school = schoolRepo.findById(schoolId);
        var schoolName = school.map(s -> s.getName()).orElse("Unknown school");
        var domain = school.map(s -> s.getDomain()).orElse(null);

        return new UserSchoolMembershipDTO(schoolId, schoolName, domain, schoolUser.getRole(),
                schoolUser.getStatus().name());
    }
}
