package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListUserBySchoolUseCase {

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;

    public Page<UserDTO> execute(String school, int page, int size) {
        // Unsorted - findAllBySchoolId bakes its own ORDER BY into the query itself. See that
        // method for why an externally-supplied Sort can't be used here.
        Pageable pageable = size > 0 ? PageRequest.of(page, size) : Pageable.unpaged();

        return repo.findBySchool(school, pageable)
                .map(user -> {
                    var memberships = schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), school);
                    List<Role> roles = memberships.stream().map(e -> e.getRole()).toList();
                    return UserMapper.toDTO(user, roles, schoolStatus(memberships));
                });
    }

    // A user can hold more than one role at the same school (e.g. Teacher and Accountant) -
    // treated as "still has access" if any one of those memberships is ACTIVE, so a page showing
    // one status per user doesn't misreport them as fully deactivated over one deactivated role.
    private String schoolStatus(List<SchoolUser> memberships) {
        if (memberships.isEmpty()) {
            return null;
        }
        boolean anyActive = memberships.stream().anyMatch(m -> m.getStatus() == SchoolUser.Status.ACTIVE);
        return anyActive ? "ACTIVE" : "INACTIVE";
    }
}
