package com.moriba.skultem.application.services;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.CommunicationAudienceRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.security.AuthUser;
import com.moriba.skultem.infrastructure.security.PermissionService;

import lombok.RequiredArgsConstructor;

// Notices, calendar events and broadcasts can be for the whole school or for one management section
// (e.g. Primary). This decides, in a school run in sections:
//   * who may POST to which audience - a section-limited Admin/Teacher only to their own section(s),
//     whole-school staff to the whole school or any one section;
//   * who SEES what - everyone sees whole-school announcements; on top of those, section-limited staff
//     see their own section's, parents see the sections their children are in, and whole-school staff
//     see everything.
// A school without sections has none of this: everything is for everyone, as before.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunicationScopeService {

    private final SchoolRepository schoolRepo;
    private final ManagementSectionRepository sectionRepo;
    private final SectionScopeService sectionScopeService;
    private final CommunicationAudienceRepository audienceRepo;

    private boolean sectionBased(String schoolId) {
        return schoolRepo.findById(schoolId).map(s -> s.getManagementModel() == ManagementModel.SECTION_BASED)
                .orElse(false);
    }

    // The sections whose announcements the caller may see besides the whole-school ones; null = no limit.
    public Set<String> visibleSectionIds(String schoolId) {
        if (!sectionBased(schoolId)) {
            return null;
        }
        var scope = sectionScopeService.currentOrAll();
        if (!scope.wholeSchool()) {
            return new HashSet<>(scope.sectionIds());
        }
        // An owner viewing one section sees whole-school announcements plus that section's.
        var view = sectionScopeService.view();
        if (view.isPresent()) {
            return new HashSet<>(view.get().sectionIds());
        }
        AuthUser user = PermissionService.getCurrentUser();
        if (user.activeRole() == Role.PARENT) {
            return audienceRepo.sectionIdsOfChildren(schoolId, user.userId());
        }
        return null;
    }

    // Whether an announcement for this section (null = whole school) is visible to the caller.
    public boolean canSee(String schoolId, String sectionId) {
        if (sectionId == null) {
            return true;
        }
        var visible = visibleSectionIds(schoolId);
        return visible == null || visible.contains(sectionId);
    }

    // The section a new announcement is for. requested may be null/blank.
    public String resolveTarget(String schoolId, String requested) {
        if (!sectionBased(schoolId)) {
            return null;
        }
        String wanted = requested == null || requested.isBlank() ? null : requested;
        if (wanted != null && sectionRepo.findBySchoolId(schoolId).stream().noneMatch(s -> s.getId().equals(wanted))) {
            throw new RuleException("Management section not found");
        }

        var scope = sectionScopeService.current();
        if (scope.wholeSchool()) {
            return wanted;
        }
        if (wanted == null) {
            if (scope.sectionIds().size() == 1) {
                return scope.sectionIds().get(0);
            }
            throw new RuleException("Choose which section this is for");
        }
        if (!scope.sectionIds().contains(wanted)) {
            throw new AccessDeniedException("Outside your management sections");
        }
        return wanted;
    }
}
