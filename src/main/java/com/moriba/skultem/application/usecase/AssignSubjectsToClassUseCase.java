package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignSubjectsToClassUseCase {
    private final ClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final SubjectGroupRepository groupRepo;
    private final ClassSubjectRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public void execute(String schoolId, String classId, List<SubjectAssignment> assignments) {

        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new RuleException("Class not found. Please select a valid class."));

        if (clazz.getLevel() == Level.SSS) {
            throw new RuleException("SSS subjects should be assigned to a stream, not directly to a class.");
        }

        assignments = assignments == null ? List.of() : assignments.stream()
                .filter(a -> a.subjectId() != null && !a.subjectId().isBlank())
                .toList();

        var subjectIds = assignments.stream()
                .map(SubjectAssignment::subjectId)
                .toList();

        if (subjectIds.size() != subjectIds.stream().distinct().count()) {
            throw new RuleException("Duplicate subjects detected.");
        }

        var existing = repo.findAllByClassIdAndSchoolId(classId, schoolId, Pageable.unpaged());

        var existingMap = existing.stream()
                .collect(Collectors.toMap(
                        cs -> cs.getSubject().getId(),
                        cs -> cs,
                        (a, b) -> {
                            throw new RuleException(
                                    "Duplicate class subject records detected. Please clean up and retry.");
                        }));

        var incomingSubjectIds = assignments.stream()
                .map(SubjectAssignment::subjectId)
                .collect(Collectors.toSet());

        for (SubjectAssignment assignment : assignments) {

            var subject = subjectRepo.findById(assignment.subjectId())
                    .orElseThrow(() -> new RuleException("Subject not found: " + assignment.subjectId()));

            if (!subject.getSchoolId().equals(schoolId)) {
                throw new RuleException(
                        "The subject '" + subject.getName() + "' does not belong to your school.");
            }

            SubjectGroup group = null;

            if (assignment.subjectGroupId() != null && !assignment.subjectGroupId().isBlank()) {
                group = groupRepo.findByIdAndClassSchoolId(
                        assignment.subjectGroupId(),
                        classId,
                        schoolId).orElseThrow(
                                () -> new RuleException("Subject group not found: " + assignment.subjectGroupId()));
            }

            if (existingMap.containsKey(assignment.subjectId())) {
                var cs = existingMap.get(assignment.subjectId());
                cs.update(subject, group, assignment.core());
                repo.save(cs);
            } else {
                var id = rg.generate("CLASS_SUBJECT", "CSJ");
                var record = ClassSubject.create(id, schoolId, clazz,
                        subject, group, assignment.core());
                repo.save(record);
            }
        }

        for (ClassSubject cs : existing) {
            if (!incomingSubjectIds.contains(cs.getSubject().getId())) {
                repo.delete(cs);
            }
        }
    }

    public record SubjectAssignment(String subjectId, String subjectGroupId, boolean core) {
    }
}
