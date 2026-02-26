package com.moriba.skultem.infrastructure.security;

import java.util.Arrays;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service("permissionService")
@RequiredArgsConstructor
public class PermissionService {

    private final StudentRepository studentRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final TeacherRepository teacherRepo;

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
        return schoolUserRepo.findBySchoolAndUser(schoolId, currentUserId()).filter(su -> su.getRole() == role)
                .isPresent();
    }

    public boolean hasAnySchoolRole(String schoolId, String... roles) {
        if (isSystemAdmin()) {
            return true;
        }
        var roleSet = Arrays.stream(roles).map(Role::valueOf).toList();
        return schoolUserRepo.findBySchoolAndUser(schoolId, currentUserId())
                .filter(su -> roleSet.contains(su.getRole())).isPresent();
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

        if (role == Role.SCHOOL_ADMIN ||
                role == Role.ACCOUNTANT ||
                role == Role.TEACHER) {
            return true;
        }

        return false;
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

        if (role == Role.SCHOOL_ADMIN ||
                role == Role.ACCOUNTANT) {
            return true;
        }

        return role == Role.TEACHER &&
                currentUserId().equals(teacher.get().getUser().getId());
    }
}
