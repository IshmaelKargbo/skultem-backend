package com.moriba.skultem.infrastructure.security;

import java.util.Arrays;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

@Service("permissionService")
@RequiredArgsConstructor
public class PermissionService {

    private final StudentRepository studentRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final TeacherRepository teacherRepo;
    private final ClassMasterRepository classMasterRepo;
    private final PromotionRequestRepository promotionRequestRepo;
    private final ParentRepository parentRepo;
    private final ReportCardRepository reportCardRepo;

    public static AuthUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new AccessDeniedException("unauthenticated");
        }

        return user;
    }

    private AuthUser currentUser() {
        return getCurrentUser();
    }

    public String currentUserId() {
        return currentUser().userId();
    }

    public String currentSchoolId() {
        return currentUser().activeSchoolId();
    }

    public Role currentRole() {
        return currentUser().activeRole();
    }

    public boolean isSystemAdmin() {
        return currentRole() == Role.SYSTEM_ADMIN;
    }

    public boolean hasRole(Role role) {
        if (isSystemAdmin()) {
            return true;
        }
        return currentRole() == role;
    }

    public boolean hasAnyRole(Role... roles) {
        if (isSystemAdmin()) {
            return true;
        }

        Role current = currentRole();

        for (Role r : roles) {
            if (r == current) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSchoolRole(String schoolId, Role role) {
        if (isSystemAdmin()) {
            return true;
        }
        return schoolUserRepo.findBySchoolAndUserAndRole(schoolId, currentUserId(), role).isPresent();
    }

    public boolean hasAnySchoolRole(String schoolId, String... roles) {
        if (isSystemAdmin()) {
            return true;
        }
        return Arrays.stream(roles)
                .map(Role::valueOf)
                .anyMatch(
                        role -> schoolUserRepo.findBySchoolAndUserAndRole(schoolId, currentUserId(), role).isPresent());
    }

    public boolean canAccessSchool(String schoolId) {
        if (isSystemAdmin()) {
            return true;
        }
        return schoolId.equals(currentSchoolId());
    }

    public boolean canAccessStudent(String studentId, String schoolId) {
        if (isSystemAdmin()) {
            return true;
        }

        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId);
        if (student.isEmpty()) {
            return false;
        }

        if (!student.get().getSchoolId().equals(currentSchoolId())) {
            return false;
        }

        Role role = currentRole();

        return role == Role.ADMIN ||
                role == Role.ACCOUNTANT ||
                role == Role.TEACHER;
    }

    public boolean canAccessTeacher(String teacherId) {
        if (isSystemAdmin()) {
            return true;
        }

        var teacher = teacherRepo.findByUserId(teacherId);
        if (teacher.isEmpty()) {
            return false;
        }

        if (!teacher.get().getSchoolId().equals(currentSchoolId())) {
            return false;
        }

        Role role = currentRole();

        if (role == Role.ADMIN ||
                role == Role.ACCOUNTANT) {
            return true;
        }

        return role == Role.TEACHER &&
                currentUserId().equals(teacher.get().getUser().getId());
    }

    // Is the signed-in PARENT actually this student's parent? Report cards (and other
    // parent-facing, per-child reads) take a studentId or a document id straight from the
    // request rather than deriving it from the caller's own children, so this is what actually
    // stops one family from viewing another's by guessing/enumerating an id - see
    // GetChildCurriculumUseCase for the same shape of check on the curriculum side.
    public boolean isParentOfStudent(String schoolId, String studentId) {
        if (!hasRole(Role.PARENT)) {
            return false;
        }

        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId);
        if (student.isEmpty() || student.get().getParent() == null) {
            return false;
        }

        var parent = parentRepo.findByUserIdAndSchoolId(currentUserId(), schoolId);
        return parent.isPresent() && parent.get().getId().equals(student.get().getParent().getId());
    }

    public boolean isParentOfReportCard(String schoolId, String reportCardId) {
        var card = reportCardRepo.findByIdAndSchoolId(reportCardId, schoolId);
        return card.isPresent() && isParentOfStudent(schoolId, card.get().getStudentId());
    }

    public boolean canPromoteClassSession(String schoolId, String sessionId) {
        if (isSystemAdmin()) {
            return true;
        }

        if (hasAnySchoolRole(schoolId, "ADMIN", "OWNER", "PROPRIETOR")) {
            return true;
        }

        if (!hasRole(Role.TEACHER)) {
            return false;
        }

        var teacher = teacherRepo.findByUserId(currentUserId());
        if (teacher.isEmpty()) {
            return false;
        }

        // A session can have more than one active class master - checking only the most recently
        // assigned one would wrongly deny a legitimate co-master who just wasn't the last one added.
        return classMasterRepo.existsByTeacherIdAndClassSessionIdAndSchoolId(teacher.get().getId(), sessionId,
                schoolId);
    }

    // "Their own classes" for a Teacher means being one of that class session's class masters (a
    // session can have more than one, e.g. co-taught classes) - the same relationship
    // ListClassSessionByTeacherUseCase uses to build a teacher's class list (the "/class-sessions/me"
    // dropdown Mark Attendance and the Daily Register both feed from), so a teacher can't view
    // another class's register just by knowing/guessing its classSessionId.
    public boolean canAccessClassSessionAsTeacher(String schoolId, String classSessionId) {
        if (!hasRole(Role.TEACHER)) {
            return false;
        }

        var teacher = teacherRepo.findByUserIdAndSchoolId(currentUserId(), schoolId);
        if (teacher.isEmpty()) {
            return false;
        }

        return classMasterRepo.existsByTeacherIdAndClassSessionIdAndSchoolId(teacher.get().getId(), classSessionId,
                schoolId);
    }

    public boolean canManagePromotionRequest(String schoolId, String requestId) {
        if (isSystemAdmin()) {
            return true;
        }

        if (hasAnySchoolRole(schoolId, "ADMIN", "OWNER", "PROPRIETOR")) {
            return true;
        }

        if (!hasRole(Role.TEACHER)) {
            return false;
        }

        var teacher = teacherRepo.findByUserId(currentUserId());
        if (teacher.isEmpty()) {
            return false;
        }

        var request = promotionRequestRepo.findByIdAndSchoolId(requestId, schoolId);

        return request.isPresent() && request.get().getMaster().getTeacher().getId().equals(teacher.get().getId());
    }
}
