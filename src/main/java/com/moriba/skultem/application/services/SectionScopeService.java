package com.moriba.skultem.application.services;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private static final String VIEW_ATTRIBUTE = SectionScopeService.class.getName() + ".view";

    // Sent by the app while an owner-level user has chosen to look at one management section.
    public static final String VIEW_HEADER = "X-View-Section";
    private static final Set<Role> VIEW_ROLES = EnumSet.of(Role.OWNER, Role.PROPRIETOR, Role.SUPER_ADMIN);

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

    // A "view one section" choice made by an owner-level user (owner / proprietor / super admin) in a school
    // run in sections - the X-View-Section header. It NARROWS what lists, reports and dashboards show to
    // that section's levels; it is never a restriction: guards, the interceptor and single-record checks
    // still see them as whole-school (so payroll, settings etc. keep working), and it is ignored for
    // anyone else and for an id that isn't one of the school's sections.
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Optional<SectionScope> view() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        if (attributes.getAttribute(VIEW_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) instanceof Optional<?> cached) {
            return (Optional<SectionScope>) cached;
        }

        Optional<SectionScope> view = Optional.empty();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user && attributes instanceof ServletRequestAttributes web) {
            String requested = web.getRequest().getHeader(VIEW_HEADER);
            if (requested != null && !requested.isBlank() && VIEW_ROLES.contains(user.activeRole())
                    && user.activeSchoolId() != null && current().wholeSchool()) {
                view = resolveView(user.activeSchoolId(), requested.trim());
            }
        }
        attributes.setAttribute(VIEW_ATTRIBUTE, view, RequestAttributes.SCOPE_REQUEST);
        return view;
    }

    private Optional<SectionScope> resolveView(String schoolId, String sectionId) {
        var school = schoolRepo.findById(schoolId).orElse(null);
        if (school == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return Optional.empty();
        }
        var section = managementSectionRepo.findBySchoolId(schoolId).stream()
                .filter(s -> s.getId().equals(sectionId)).findFirst().orElse(null);
        if (section == null) {
            return Optional.empty();
        }
        Set<Level> levels = EnumSet.noneOf(Level.class);
        schoolLevelRepo.findBySchoolId(schoolId).stream()
                .filter(l -> sectionId.equals(l.getManagementSectionId()))
                .forEach(l -> levels.add(l.getLevel()));
        return levels.isEmpty() ? Optional.empty()
                : Optional.of(SectionScope.sections(levels, List.of(section.getId()), List.of(section.getName())));
    }

    // What list / report / dashboard queries filter by: the chosen section view if there is one, else the
    // caller's own scope.
    public SectionScope effective() {
        return view().orElseGet(this::currentOrAll);
    }

    // Shorthand for list queries: `... and x.clazz.level in :levels`.
    public Collection<Level> levels() {
        return effective().queryLevels();
    }

    // For filters that go through a sub-query (e.g. "student enrolled this year at these levels"):
    // null for whole-school callers so they skip the filter entirely - applying it with every level
    // would still drop rows for students with no enrollment that year, changing what they see today.
    public Collection<Level> restrictedLevels() {
        var scope = effective();
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
