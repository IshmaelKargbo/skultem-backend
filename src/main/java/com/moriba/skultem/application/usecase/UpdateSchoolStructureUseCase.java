package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.SchoolStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

// Replaces a school's whole structure in one go - offered levels, management model and management
// sections - so every rule is checked against the complete picture rather than piece by piece:
//   * at least one level; a level that still has classes can't be dropped (they'd be orphaned);
//   * SECTION_BASED: every offered level is managed by exactly one section, every section manages
//     at least one level, and section names are unique within the school;
//   * UNIFIED: no sections at all - the whole school is one management scope.
// Sections keep their ids across edits (matched by id), so anything pointing at a section later
// (staff scope) survives a rename; an id that isn't one of this school's sections is rejected.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSchoolStructureUseCase {

    public record SectionInput(String id, String name, List<Level> levels) {
    }

    private static final int MAX_SECTION_NAME = 100;

    private final SchoolRepository schoolRepo;
    private final SchoolLevelRepository schoolLevelRepo;
    private final ManagementSectionRepository managementSectionRepo;
    private final ClassRepository classRepo;
    private final StaffManagementSectionRepository staffSectionRepo;
    private final GetSchoolStructureUseCase getSchoolStructureUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SCHOOL_STRUCTURE_UPDATED")
    public SchoolStructureDTO execute(String schoolId, ManagementModel model, List<Level> levels,
            List<SectionInput> sections) {
        var school = schoolRepo.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));

        if (model == null) {
            throw new RuleException("Choose how the school is managed");
        }
        Set<Level> offered = toLevelSet(levels);
        if (offered.isEmpty()) {
            throw new RuleException("Select at least one school level");
        }

        List<SectionInput> requested = model == ManagementModel.SECTION_BASED
                ? validateSections(offered, sections == null ? List.of() : sections)
                : List.of();

        // Levels: add new ones, drop removed ones (only when no classes use them).
        Map<Level, SchoolLevel> existingLevels = schoolLevelRepo.findBySchoolId(schoolId).stream()
                .collect(Collectors.toMap(SchoolLevel::getLevel, Function.identity(), (a, b) -> a,
                        () -> new EnumMap<>(Level.class)));

        for (var existing : existingLevels.values()) {
            if (offered.contains(existing.getLevel())) {
                continue;
            }
            int classes = classRepo.countActiveBySchoolAndLevel(schoolId, existing.getLevel());
            if (classes > 0) {
                throw new RuleException(existing.getLevel().getLabel() + " still has " + classes
                        + (classes == 1 ? " class" : " classes")
                        + ". Delete or move them before removing " + existing.getLevel().getLabel() + ".");
            }
        }

        // Sections: match by id, create new, delete the rest. Saved before the levels that point at them.
        Map<String, ManagementSection> existingSections = managementSectionRepo.findBySchoolId(schoolId).stream()
                .collect(Collectors.toMap(ManagementSection::getId, Function.identity()));

        // A section staff are limited to can't just disappear: their scope rows would point nowhere
        // and they'd fall back to whole-school access. Their access has to be changed first.
        Set<String> requestedIds = requested.stream().map(SectionInput::id).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (var section : existingSections.values()) {
            if (!requestedIds.contains(section.getId()) && staffSectionRepo.existsBySectionId(section.getId())) {
                throw new RuleException("Staff are still limited to \"" + section.getName() + "\". Change their "
                        + "management access (Users) before "
                        + (model == ManagementModel.UNIFIED ? "switching to one management for the whole school."
                                : "removing this section."));
            }
        }

        Map<Level, String> sectionOfLevel = new EnumMap<>(Level.class);
        Set<String> keptSectionIds = new HashSet<>();
        for (int i = 0; i < requested.size(); i++) {
            var input = requested.get(i);
            String name = input.name().trim();

            ManagementSection section;
            if (input.id() != null && !input.id().isBlank()) {
                section = existingSections.get(input.id());
                if (section == null) {
                    throw new RuleException("Management section not found: " + name);
                }
                section.update(name, i);
            } else {
                section = ManagementSection.create(schoolId, name, i);
            }
            managementSectionRepo.save(section);
            keptSectionIds.add(section.getId());
            for (var level : input.levels()) {
                sectionOfLevel.put(level, section.getId());
            }
        }

        for (var level : offered) {
            var row = existingLevels.get(level);
            if (row == null) {
                row = SchoolLevel.create(schoolId, level);
            }
            row.assignTo(sectionOfLevel.get(level));
            schoolLevelRepo.save(row);
        }

        for (var existing : existingLevels.values()) {
            if (!offered.contains(existing.getLevel())) {
                schoolLevelRepo.delete(existing);
            }
        }

        for (var section : existingSections.values()) {
            if (!keptSectionIds.contains(section.getId())) {
                managementSectionRepo.delete(section);
            }
        }

        school.setManagementModel(model);
        schoolRepo.save(school);

        logActivityUseCase.log(schoolId, ActivityType.SETTINGS, "School structure updated",
                offered.stream().map(Level::getLabel).collect(Collectors.joining(", ")),
                model == ManagementModel.UNIFIED ? "One management for the whole school"
                        : requested.size() + " management sections",
                schoolId);

        return getSchoolStructureUseCase.execute(schoolId);
    }

    private List<SectionInput> validateSections(Set<Level> offered, List<SectionInput> sections) {
        if (sections.isEmpty()) {
            throw new RuleException("Add at least one management section");
        }

        Set<String> names = new HashSet<>();
        Map<Level, String> owner = new EnumMap<>(Level.class);
        List<SectionInput> result = new ArrayList<>();

        for (var section : sections) {
            String name = section.name() == null ? "" : section.name().trim();
            if (name.isEmpty()) {
                throw new RuleException("Every management section needs a name");
            }
            if (name.length() > MAX_SECTION_NAME) {
                throw new RuleException("Management section names can be at most " + MAX_SECTION_NAME + " characters");
            }
            if (!names.add(name.toLowerCase())) {
                throw new RuleException("There is more than one management section named \"" + name + "\"");
            }
            Set<Level> sectionLevels = toLevelSet(section.levels());
            if (sectionLevels.isEmpty()) {
                throw new RuleException("\"" + name + "\" needs at least one school level");
            }
            for (var level : sectionLevels) {
                if (!offered.contains(level)) {
                    throw new RuleException("\"" + name + "\" includes " + level.getLabel()
                            + ", which the school doesn't offer");
                }
                var previous = owner.put(level, name);
                if (previous != null && !previous.equals(name)) {
                    throw new RuleException(level.getLabel() + " can only belong to one management section (it's in \""
                            + previous + "\" and \"" + name + "\")");
                }
            }
            result.add(new SectionInput(section.id(), name, List.copyOf(sectionLevels)));
        }

        var unassigned = offered.stream().filter(l -> !owner.containsKey(l)).map(Level::getLabel).toList();
        if (!unassigned.isEmpty()) {
            throw new RuleException("Assign every school level to a management section - not yet assigned: "
                    + String.join(", ", unassigned));
        }

        return result;
    }

    private static Set<Level> toLevelSet(List<Level> levels) {
        Set<Level> set = EnumSet.noneOf(Level.class);
        if (levels != null) {
            levels.stream().filter(Objects::nonNull).forEach(set::add);
        }
        return set;
    }
}
