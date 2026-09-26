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
    private static final Set<Role> STAFF_ROLES = Set.of(Role.ADMIN, Role.SUPER_ADMIN, Role.ACCOUNTANT, Role.PROPRIETOR, Role.OWNER);

    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final TeacherRepository teacherRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final com.moriba.skultem.domain.repository.CommunicationAudienceRepository audienceRepo;
    private final com.moriba.skultem.application.services.CommunicationScopeService scopeService;

    // For the signed-in caller: the section they're addressing (a section-limited Admin's own by default).
    public int forCaller(String schoolId, Audience audience, String requestedSectionId) {
        return execute(schoolId, audience, scopeService.resolveTarget(schoolId, requestedSectionId));
    }

    // Reach of an announcement for one management section (null = the whole school).
    public int execute(String schoolId, Audience audience, String sectionId) {
        if (sectionId == null) {
            return execute(schoolId, audience);
        }
        var c = audienceRepo.countsForSection(schoolId, sectionId);
        return switch (audience) {
            case STUDENTS -> c.students();
            case PARENTS -> c.parents();
            case TEACHERS -> c.teachers();
            case STAFF -> c.staff();
            case ALL -> c.students() + c.parents() + c.teachers() + c.staff();
        };
    }

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
