package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The school's own ADMIN/OWNER/PROPRIETOR revoking one specific role from a user at their school
// (e.g. someone no longer needs Accountant access but keeps Teacher) - the counterpart to
// AssignRoleUseCase, which grants a role. Distinct from SetUserAccessUseCase, which
// (de)activates every role together rather than removing one permanently.
@Service
@Transactional
@RequiredArgsConstructor
public class RemoveRoleUseCase {

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final TeacherRepository teacherRepo;
    private final UserSessionRepository sessionRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ROLE_REMOVED")
    public UserDTO execute(String schoolId, String targetUserId, String actingUserId, Role role) {

        if (role == Role.SYSTEM_ADMIN) {
            throw new RuleException("System admin accounts can't be managed from here.");
        }

        if (targetUserId.equals(actingUserId)) {
            throw new RuleException("You can't remove your own role.");
        }

        var user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var membership = schoolUserRepo.findBySchoolAndUserAndRole(schoolId, targetUserId, role)
                .orElseThrow(() -> new NotFoundException("This user doesn't hold that role at your school."));

        schoolUserRepo.delete(membership);

        // Removing TEACHER also takes them off the classroom-teaching side of their staff record,
        // if they have one - keeps Teacher/payroll status from silently disagreeing with what
        // roles they actually still hold.
        if (role == Role.TEACHER) {
            teacherRepo.findByUserIdAndSchoolId(targetUserId, schoolId).ifPresent(teacher -> {
                teacher.deactivate();
                teacherRepo.save(teacher);
            });
        }

        var remaining = schoolUserRepo.findAllByUser_IdAndSchoolId(targetUserId, schoolId);

        // That may have been their only role here - if so they have no access left at all, so
        // sign out any session they're currently using rather than leaving it valid until it
        // expires on its own.
        boolean stillHasAccess = remaining.stream().anyMatch(m -> m.getStatus() == SchoolUser.Status.ACTIVE);
        if (!stillHasAccess) {
            sessionRepo.findAllByUserAndSchoolIdAndActive(targetUserId, schoolId, true)
                    .forEach(session -> {
                        session.deactivate();
                        sessionRepo.save(session);
                    });
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.USER,
                "Role removed",
                user.getGivenNames() + " " + user.getFamilyName() + " (" + role.name() + ")",
                null,
                user.getId());

        List<Role> roles = remaining.stream().map(SchoolUser::getRole).toList();
        String schoolStatus = remaining.isEmpty() ? null : (stillHasAccess ? "ACTIVE" : "INACTIVE");

        return UserMapper.toDTO(user, roles, schoolStatus);
    }
}
