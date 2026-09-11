package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The school's own ADMIN/OWNER/PROPRIETOR deactivating (or reactivating) any user at their
// school - not just a Teacher/payroll record (see SetTeacherStatusUseCase for that narrower,
// staff-specific action, which this stays in sync with when the target also has a Teacher record).
// Unlike SetSchoolUserStatusUseCase (system-admin-only, works off one SchoolUser id directly and
// touches one role at a time), this is scoped to the acting admin's own school and flips every
// role the target holds *at that school* together - the natural "deactivate this person" action
// when the caller isn't picking a specific role.
@Service
@Transactional
@RequiredArgsConstructor
public class SetUserAccessUseCase {

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final TeacherRepository teacherRepo;
    private final UserSessionRepository sessionRepo;
    private final LogActivityUseCase logActivityUseCase;

    public UserDTO execute(String schoolId, String targetUserId, String actingUserId, boolean activate) {

        if (targetUserId.equals(actingUserId)) {
            throw new RuleException("You can't deactivate your own account.");
        }

        var user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var memberships = schoolUserRepo.findAllByUser_IdAndSchoolId(targetUserId, schoolId);

        if (memberships.isEmpty()) {
            throw new NotFoundException("This user doesn't belong to your school.");
        }

        if (memberships.stream().anyMatch(m -> m.getRole() == Role.SYSTEM_ADMIN)) {
            throw new RuleException("System admin accounts can't be managed from here.");
        }

        var targetStatus = activate ? SchoolUser.Status.ACTIVE : SchoolUser.Status.INACTIVE;

        for (SchoolUser membership : memberships) {
            if (membership.getStatus() != targetStatus) {
                membership.setStatus(targetStatus);
                schoolUserRepo.save(membership);
            }
        }

        // Keep the Teacher/payroll record in step too, if this user has one at this school -
        // whichever page an admin uses (this one, or the staff profile's own toggle), the two
        // stay consistent instead of drifting apart.
        teacherRepo.findByUserIdAndSchoolId(targetUserId, schoolId).ifPresent(teacher -> {
            if (activate) {
                teacher.activate();
            } else {
                teacher.deactivate();
            }
            teacherRepo.save(teacher);
        });

        if (!activate) {
            sessionRepo.findAllByUserAndSchoolIdAndActive(targetUserId, schoolId, true)
                    .forEach(session -> {
                        session.deactivate();
                        sessionRepo.save(session);
                    });
        }

        List<Role> roles = memberships.stream().map(SchoolUser::getRole).toList();

        logActivityUseCase.log(
                schoolId,
                ActivityType.USER,
                activate ? "User access restored" : "User access revoked",
                user.getGivenNames() + " " + user.getFamilyName(),
                null,
                user.getId());

        return UserMapper.toDTO(user, roles, targetStatus.name());
    }
}
