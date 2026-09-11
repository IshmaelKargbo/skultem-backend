package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Copies one class's subject list (core + optional/grouped) onto one or more other classes, e.g.
 * duplicating Nursery 1's subjects onto Nursery 2 instead of re-assigning them one by one.
 *
 * Each target is assigned independently via {@link AssignSubjectsToClassUseCase} so it inherits
 * the same locking, mandatory-for-PRIMARY, and enrolled-student-sync rules as a manual assignment
 * - a target that fails (e.g. a locked subject would be removed) doesn't stop the others.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DuplicateClassSubjectsUseCase {

    private final ClassSessionRepository sessionRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final SubjectGroupRepository subjectGroupRepo;
    private final AssignSubjectsToClassUseCase assignSubjectsToClassUseCase;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "DUPLICATED_CLASS_SUBJECTS")
    public DuplicateResult execute(String schoolId, String sourceClassId, List<String> targetClassIds) {

        var sourceSession = sessionRepo.findByIdAndSchoolId(sourceClassId, schoolId)
                .orElseThrow(() -> new RuleException("Source class not found."));
        var sourceClazz = sourceSession.getClazz();
        var sourceStream = sourceSession.getStream();

        // A streamed class (e.g. SSS's Art/Science/Commercial) shares one Clazz across every
        // stream, so querying by clazz id alone would pull every stream's subjects together - if
        // the same subject (e.g. English) is core in more than one stream, it'd show up twice and
        // trip AssignSubjectsToClassUseCase's "Duplicate subjects detected" check on the target.
        var sourceSubjects = sourceStream != null
                ? classSubjectRepo
                        .findAllByClassIdAndStreamIdAndSchoolId(sourceClazz.getId(), sourceStream.getId(), schoolId,
                                Pageable.unpaged())
                        .getContent()
                : classSubjectRepo
                        .findAllByClassIdAndSchoolId(sourceClazz.getId(), schoolId, Pageable.unpaged())
                        .getContent();

        if (sourceSubjects.isEmpty()) {
            throw new RuleException(
                    "'" + sourceSession.getName() + "' has no subjects assigned yet, so there's nothing to duplicate.");
        }

        var distinctTargets = (targetClassIds == null ? List.<String>of() : targetClassIds).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();

        if (distinctTargets.isEmpty()) {
            throw new RuleException("Select at least one class to duplicate the subjects into.");
        }

        List<String> succeeded = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();

        for (String targetClassId : distinctTargets) {

            if (targetClassId.equals(sourceClassId)) {
                failed.put(targetClassId, "Can't duplicate a class onto itself.");
                continue;
            }

            try {
                var targetSession = sessionRepo.findByIdAndSchoolId(targetClassId, schoolId)
                        .orElseThrow(() -> new RuleException("Target class not found."));
                var targetClazz = targetSession.getClazz();

                Map<String, String> groupIdMap = duplicateGroups(schoolId, sourceSubjects, targetClazz);

                var assignments = sourceSubjects.stream()
                        .map(cs -> new AssignSubjectsToClassUseCase.SubjectAssignment(
                                cs.getSubject().getId(),
                                cs.getGroup() != null ? groupIdMap.get(cs.getGroup().getId()) : null,
                                Boolean.TRUE.equals(cs.getMandatory())))
                        .toList();

                assignSubjectsToClassUseCase.execute(schoolId, targetClassId, assignments);
                succeeded.add(targetClassId);
            } catch (RuleException e) {
                failed.put(targetClassId, e.getMessage());
            }
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.SUBJECT,
                "Class subjects duplicated",
                sourceSession.getName(),
                "targets=" + distinctTargets.size() + ";succeeded=" + succeeded.size() + ";failed=" + failed.size(),
                sourceClazz.getId());

        return new DuplicateResult(succeeded, failed);
    }

    // Subject groups belong to one specific class, so a source group's id can't just be reused on
    // the target - a matching group (by name) is reused if the target already has one, otherwise a
    // new one is created for the target so the elective structure carries over too. PRIMARY classes
    // can't have subject groups at all (see CreateSubjectGroupUseCase), so those are skipped there
    // and the subject just lands as ungrouped/core on the target, matching how PRIMARY already
    // forces every subject to be core.
    private Map<String, String> duplicateGroups(String schoolId, List<ClassSubject> sourceSubjects,
            Clazz targetClazz) {

        var sourceGroups = sourceSubjects.stream()
                .map(ClassSubject::getGroup)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(SubjectGroup::getId, g -> g, (a, b) -> a));

        if (sourceGroups.isEmpty() || targetClazz.getLevel() == Level.PRIMARY) {
            return Map.of();
        }

        var existingTargetGroups = subjectGroupRepo
                .findByClassAndSchool(targetClazz.getId(), schoolId, Pageable.unpaged())
                .getContent();

        var existingByName = existingTargetGroups.stream()
                .collect(Collectors.toMap(g -> g.getName().trim().toLowerCase(), g -> g, (a, b) -> a));

        Map<String, String> groupIdMap = new HashMap<>();

        for (var group : sourceGroups.values()) {
            var match = existingByName.get(group.getName().trim().toLowerCase());

            if (match != null) {
                groupIdMap.put(group.getId(), match.getId());
                continue;
            }

            var newGroupId = rg.generate("SUBJECT_GROUP", "SBG");
            var newGroup = SubjectGroup.create(newGroupId, schoolId, group.getName(), targetClazz, null,
                    group.getTotalSelection());
            subjectGroupRepo.save(newGroup);
            groupIdMap.put(group.getId(), newGroupId);
        }

        return groupIdMap;
    }

    public record DuplicateResult(List<String> succeeded, Map<String, String> failed) {
    }
}
