package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.NotFoundException;
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
public class GetUserUseCase {

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;

    public UserDTO execute(String schoolId, String id) {
        var record = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        // A session with no school is a SYSTEM_ADMIN one (see SystemAdminLoginUseCase) - the
        // school-scoped lookup does "school_id = NULL", which never matches, so this returned no
        // roles at all and the portal showed such an admin nothing but the dashboard. Same
        // handling as RefreshTokenUseCase.
        var memberships = schoolId == null
                ? schoolUserRepo.findAllByUser_Id(id).stream()
                        .filter(su -> su.getRole() == Role.SYSTEM_ADMIN && su.getSchoolId() == null)
                        .toList()
                : schoolUserRepo.findAllByUser_IdAndSchoolId(id, schoolId);
        List<Role> roles = memberships.stream().map(e -> e.getRole()).toList();
        String schoolStatus = memberships.isEmpty() ? null
                : memberships.stream().anyMatch(m -> m.getStatus() == SchoolUser.Status.ACTIVE) ? "ACTIVE"
                        : "INACTIVE";
        return UserMapper.toDTO(record, roles, schoolStatus);
    }
}
