package com.moriba.skultem.application.services;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.domain.vo.SectionScope;
import com.moriba.skultem.infrastructure.security.AuthUser;
import com.moriba.skultem.infrastructure.security.PermissionService;

import lombok.RequiredArgsConstructor;

// Works out a caller's SectionScope. Only ADMIN, ACCOUNTANT and TEACHER can be limited to management
// sections; everyone else (owner, proprietor, super admin, parent, system admin) is whole-school.
@Service
@RequiredArgsConstructor
public class SectionScopeService {

    public static final Set<Role> SCOPABLE_ROLES = EnumSet.of(Role.ADMIN, Role.ACCOUNTANT, Role.TEACHER);

    private static final String REQUEST_ATTRIBUTE = SectionScopeService.class.getName() + ".scope";

    private final SchoolRepository schoolRepo;
    private final SchoolLevelRepository schoolLevelRepo;
    private final ManagementSectionRepository managementSectionRepo;
    private final StaffManagementSectionRepository staffSectionRepo;

    // The signed-in caller's scope, resolved once per request.
    @Transactional(readOnly = true)
    public SectionScope current() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null
                && attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) instanceof SectionScope cached) {
            return cached;
        }

        AuthUser user = PermissionService.getCurrentUser();
        var scope = resolve(user.activeSchoolId(), user.userId(), user.activeRole());

        if (attributes != null) {
            attributes.setAttribute(REQUEST_ATTRIBUTE, scope, RequestAttributes.SCOPE_REQUEST);
        }
        return scope;
    }

    // For use cases that filter lists: the caller's scope inside a request, whole-school for work
    // with no signed-in user (startup sweeps, scheduled jobs), which isn't acting for anyone.
    @Transactional(readOnly = true)
    public SectionScope currentOrAll() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser)) {
            return SectionScope.all();
        }
        return current();
    }

    // Shorthand for list queries: `... and x.clazz.level in :levels`.
    public Collection<Level> levels() {
        return currentOrAll().queryLevels();
    }

    // For filters that go through a sub-query (e.g. "student enrolled this year at these levels"):
    // null for whole-school callers so they skip the filter entirely - applying it with every level
    // would still drop rows for students with no enrollment that year, changing what they see today.
    public Collection<Level> restrictedLevels() {
        var scope = currentOrAll();
        return scope.wholeSchool() ? null : scope.queryLevels();
    }

    @Transactional(readOnly = true)
    public SectionScope resolve(String schoolId, String userId, Role role) {
        if (schoolId == null || role == null || !SCOPABLE_ROLES.contains(role)) {
            return SectionScope.all();
        }

        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return SectionScope.all();
        }

        List<StaffManagementSection> assigned = staffSectionRepo.findBySchoolAndUserAndRole(schoolId, userId, role);
        if (assigned.isEmpty()) {
            return SectionScope.all();
        }

        Set<String> sectionIds = new HashSet<>();
        assigned.forEach(a -> sectionIds.add(a.getManagementSectionId()));

        Set<Level> levels = EnumSet.noneOf(Level.class);
        schoolLevelRepo.findBySchoolId(schoolId).stream()
                .filter(l -> l.getManagementSectionId() != null && sectionIds.contains(l.getManagementSectionId()))
                .forEach(l -> levels.add(l.getLevel()));

        var sections = managementSectionRepo.findBySchoolId(schoolId).stream()
                .filter(s -> sectionIds.contains(s.getId()))
                .toList();

        return SectionScope.sections(levels, sections.stream().map(ManagementSection::getId).toList(),
                sections.stream().map(ManagementSection::getName).toList());
    }
}
