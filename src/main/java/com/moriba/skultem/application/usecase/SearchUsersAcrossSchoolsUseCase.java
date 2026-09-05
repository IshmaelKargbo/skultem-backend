package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserSchoolMembershipDTO;
import com.moriba.skultem.application.dto.UserWithSchoolsDTO;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * System-admin-only cross-tenant user lookup - e.g. helping a locked-out school owner, or
 * checking which schools an email already belongs to before onboarding them elsewhere. Every
 * other user search in the app (ListUserBySchoolUseCase included) is scoped to one school; this
 * is deliberately not, which is why it lives behind SystemAdminController's isSystemAdmin() gate
 * rather than the usual hasAnySchoolRole(school, ...) one.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SearchUsersAcrossSchoolsUseCase {

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final SchoolRepository schoolRepo;

    public Page<UserWithSchoolsDTO> execute(String query, int page, int size) {
        Pageable pageable = size > 0 ? PageRequest.of(page, size) : Pageable.unpaged();

        return userRepo.search(query, pageable).map(user -> {
            var memberships = schoolUserRepo.findAllByUser_Id(user.getId()).stream()
                    .map(su -> {
                        // A school can be deleted/renamed without cleaning up old memberships
                        // elsewhere in this codebase (see e.g. FeeStructure), so this tolerates a
                        // dangling schoolId the same way rather than failing the whole search.
                        var school = schoolRepo.findById(su.getSchoolId());
                        var schoolName = school.map(s -> s.getName()).orElse("Unknown school");
                        var domain = school.map(s -> s.getDomain()).orElse(null);
                        return new UserSchoolMembershipDTO(su.getSchoolId(), schoolName, domain, su.getRole(),
                                su.getStatus().name());
                    })
                    .toList();

            return new UserWithSchoolsDTO(user.getId(), user.getGivenNames(), user.getFamilyName(), user.getEmail(),
                    user.getPhoto(), memberships);
        });
    }
}
