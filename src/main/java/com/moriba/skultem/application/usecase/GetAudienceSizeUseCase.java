package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Audience;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAudienceSizeUseCase {
    private static final Set<Role> STAFF_ROLES = Set.of(Role.ADMIN, Role.ACCOUNTANT, Role.PROPRIETOR, Role.OWNER);

    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final TeacherRepository teacherRepo;
    private final SchoolUserRepository schoolUserRepo;

    public int execute(String schoolId, Audience audience) {
        long students = studentRepo.findBySchoolId(schoolId, Pageable.unpaged()).getTotalElements();
        long parents = parentRepo.findBySchool(schoolId, Pageable.unpaged()).getTotalElements();
        long teachers = teacherRepo.countAllBySchool(schoolId);
        long staff = schoolUserRepo.findBySchool(schoolId).stream()
                .filter(su -> STAFF_ROLES.contains(su.getRole()))
                .count();

        return switch (audience) {
            case STUDENTS -> (int) students;
            case PARENTS -> (int) parents;
            case TEACHERS -> (int) teachers;
            case STAFF -> (int) staff;
            case ALL -> (int) (students + parents + teachers + staff);
        };
    }
}
