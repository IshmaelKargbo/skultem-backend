package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.mail.MailService;
import com.moriba.skultem.infrastructure.mail.MailService.AssignUserPayload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignRoleUseCase {

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;
    private final SchoolRepository schoolRepo;
    private final LogActivityUseCase logActivityUseCase;
    private final MailService mailService;

    @AuditLogAnnotation(action = "ROLE_ASSIGN")
    public UserDTO execute(String schoolId, String userId, String role) {
        var user = repo.findById(userId).orElseThrow(() -> new NotFoundException("no user found"));
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));
        if (schoolUserRepo.existsBySchoolAndUserAndRole(schoolId, user.getId(), Role.valueOf(role))) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var roleEnum = Role.valueOf(role);
        var schoolUser = SchoolUser.create(schoolId, user, roleEnum);

        List<Role> roles = schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), schoolId).stream()
                .map(e -> e.getRole())
                .toList();
        schoolUserRepo.save(schoolUser);

        logActivityUseCase.log(
                schoolId,
                ActivityType.USER,
                "New role assign",
                user.getGivenNames() + " " + user.getFamilyName() + " (" + roleEnum.name() + ")",
                null,
                user.getId());
        sendAssignEmail(school, user, role);
        return UserMapper.toDTO(user, roles);
    }

    private void sendAssignEmail(School school, User user, String role) {
        var subdomain = school.getDomain() + ".skultem.space";
        var link = "https://" + subdomain + "/login";
        var param = new AssignUserPayload(user.getEmail(), user.getName(), link, school.getName(), role, subdomain);
        mailService.sendAssignUserEmail(param);
    }

}
