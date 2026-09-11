package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Deactivating a staff member (resigned, fired, or a temporary suspension) - and reactivating them
// later. This is the school's own admin-facing counterpart to SetSchoolUserStatusUseCase (which is
// system-admin-only and works off a SchoolUser id directly): it keeps the Teacher/payroll record
// and that person's TEACHER portal membership in sync from the one staff record, and kills any
// session they're currently logged in with so a deactivation takes effect immediately rather than
// on their next login - LoginUseCase already rejects a non-ACTIVE SchoolUser, so without this a
// deactivated teacher could keep using an already-open session indefinitely.
@Service
@Transactional
@RequiredArgsConstructor
public class SetTeacherStatusUseCase {

    private final TeacherRepository repo;
    private final SchoolUserRepository schoolUserRepo;
    private final UserSessionRepository sessionRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_STATUS_CHANGED")
    public TeacherDTO execute(String schoolId, String teacherId, String actingUserId, boolean activate) {

        var teacher = repo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        if (teacher.getStatus() == Teacher.Status.DELETED) {
            throw new RuleException("This staff record has been deleted.");
        }

        if (!activate && teacher.getUser().getId().equals(actingUserId)) {
            throw new RuleException("You can't deactivate your own account.");
        }

        boolean alreadyInState = activate
                ? teacher.getStatus() == Teacher.Status.ACTIVE
                : teacher.getStatus() == Teacher.Status.INACTIVE;

        if (alreadyInState) {
            return TeacherMapper.toDTO(teacher);
        }

        if (activate) {
            teacher.activate();
        } else {
            teacher.deactivate();
        }
        repo.save(teacher);

        // Best-effort - a Teacher always gets a TEACHER SchoolUser row when created (see
        // CreateTeacherUseCase), but this doesn't assume it's still there.
        schoolUserRepo.findBySchoolAndUserAndRole(schoolId, teacher.getUser().getId(), Role.TEACHER)
                .ifPresent(schoolUser -> {
                    schoolUser.setStatus(activate ? SchoolUser.Status.ACTIVE : SchoolUser.Status.INACTIVE);
                    schoolUserRepo.save(schoolUser);
                });

        if (!activate) {
            sessionRepo.findAllByUserAndSchoolIdAndActive(teacher.getUser().getId(), schoolId, true)
                    .forEach(session -> {
                        session.deactivate();
                        sessionRepo.save(session);
                    });
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.TEACHER,
                activate ? "Staff reactivated" : "Staff deactivated",
                teacher.getUser().getName() + " - " + teacher.getStaffId(),
                null,
                teacher.getId());

        return TeacherMapper.toDTO(teacher);
    }
}
