package com.moriba.skultem.application.usecase;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.StaffScopeDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

// Limits a staff member (under one role) to some management sections, or - with an empty list -
// gives them the whole school again. Only for ADMIN/ACCOUNTANT/TEACHER in a SECTION_BASED school,
// and only sections of that same school.
@Service
@Transactional
@RequiredArgsConstructor
public class AssignStaffManagementSectionsUseCase {

    private final SchoolRepository schoolRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final ManagementSectionRepository managementSectionRepo;
    private final StaffManagementSectionRepository staffSectionRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STAFF_MANAGEMENT_SCOPE_UPDATED")
    public StaffScopeDTO execute(String schoolId, String userId, Role role, List<String> sectionIds) {
        if (role == null || !SectionScopeService.SCOPABLE_ROLES.contains(role)) {
            throw new RuleException("Only admins, accountants and teachers can be limited to management sections");
        }

        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        var requested = new HashSet<>(sectionIds == null ? List.<String>of() : sectionIds);
        requested.remove(null);

        if (!requested.isEmpty() && school.getManagementModel() != ManagementModel.SECTION_BASED) {
            throw new RuleException(
                    "This school has one management for the whole school. Set up management sections first.");
        }

        var membership = schoolUserRepo.findBySchoolAndUserAndRole(schoolId, userId, role)
                .orElseThrow(() -> new NotFoundException("This user doesn't have the " + role.name() + " role here"));

        var sections = managementSectionRepo.findBySchoolId(schoolId);
        var known = sections.stream().map(ManagementSection::getId).collect(Collectors.toSet());
        if (!known.containsAll(requested)) {
            // Also what stops granting a section of another school.
            throw new RuleException("One of the management sections doesn't belong to this school");
        }

        var ordered = sections.stream().map(ManagementSection::getId).filter(requested::contains).toList();
        staffSectionRepo.replace(schoolId, userId, role, ordered);

        var names = sections.stream().filter(s -> requested.contains(s.getId())).map(ManagementSection::getName)
                .collect(Collectors.joining(", "));
        logActivityUseCase.log(schoolId, ActivityType.SETTINGS, "Management access updated",
                membership.getUser().getGivenNames() + " " + membership.getUser().getFamilyName() + " (" + role.name() + ")",
                names.isEmpty() ? "Whole school" : names, userId);

        return new StaffScopeDTO(userId, role, ordered);
    }
}
