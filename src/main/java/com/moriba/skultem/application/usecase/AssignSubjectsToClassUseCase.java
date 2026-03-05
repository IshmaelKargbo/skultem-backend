package com.moriba.skultem.application.usecase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.EnrollmentSubject;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

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
    private final EnrollmentRepository enrollmentRepo;
    private final EnrollmentSubjectRepository enrollmentSubjectRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;
    private final ReferenceGeneratorUsecase rg;

    public void execute(String schoolId, String classId, List<SubjectAssignment> assignments) {

        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new RuleException("Class not found. Please select a valid class."));

        if (clazz.getLevel() == Level.SSS) {
            throw new RuleException("SSS subjects should be assigned to a stream, not directly to a class.");
        }

        assignments = assignments == null ? List.of()
                : assignments.stream()
                        .filter(a -> a.subjectId() != null && !a.subjectId().isBlank())
                        .toList();

        var subjectIds = assignments.stream()
                .map(SubjectAssignment::subjectId)
                .toList();

        if (subjectIds.size() != subjectIds.stream().distinct().count()) {
            throw new RuleException("Duplicate subjects detected.");
        }

        var existing = repo.findAllByClassIdAndSchoolId(classId, schoolId, Pageable.unpaged());

        // Lock existing subjects that have grade activity
        existing.forEach(item -> {
            if (item.isLocked()) {
                return;
            }

            boolean hasGradeActivity = assessmentScoreRepository
                    .existsGradeActivityByClassIdAndSubjectIdAndSchoolId(classId, item.getSubject().getId(), schoolId);

            if (hasGradeActivity) {
                item.lock();
                repo.save(item);
            }
        });

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

        Set<String> removedSubjectIds = existing.stream()
                .map(cs -> cs.getSubject().getId())
                .collect(Collectors.toCollection(HashSet::new));
        removedSubjectIds.removeAll(incomingSubjectIds);

        Map<String, Subject> incomingSubjects = new HashMap<>();

        // Process incoming assignments
        for (SubjectAssignment assignment : assignments) {

            var subject = subjectRepo.findById(assignment.subjectId())
                    .orElseThrow(() -> new RuleException("Subject not found: " + assignment.subjectId()));

            if (!subject.getSchoolId().equals(schoolId)) {
                throw new RuleException(
                        "The subject '" + subject.getName() + "' does not belong to your school.");
            }
            incomingSubjects.put(subject.getId(), subject);

            SubjectGroup group = null;

            if (assignment.subjectGroupId() != null && !assignment.subjectGroupId().isBlank()) {
                group = groupRepo.findByIdAndClassSchoolId(
                        assignment.subjectGroupId(),
                        classId,
                        schoolId)
                        .orElseThrow(
                                () -> new RuleException("Subject group not found: " + assignment.subjectGroupId()));
            }

            boolean core = clazz.getLevel() == Level.PRIMARY
                    ? true
                    : assignment.core();

            if (existingMap.containsKey(assignment.subjectId())) {
                var cs = existingMap.get(assignment.subjectId());

                if (cs.isLocked() && hasClassSubjectChanged(cs, assignment.subjectGroupId(), core)) {
                    throw new RuleException(
                            "Subject '" + cs.getSubject().getName()
                                    + "' is locked because student grades exist. It can only be viewed.");
                }

                if (cs.isLocked()) {
                    continue;
                }

                cs.update(subject, group, core);
                repo.save(cs);
            } else {
                var record = ClassSubject.create(
                        UUID.randomUUID().toString(), schoolId, clazz, subject, group, core);
                repo.save(record);
            }
        }

        // Remove subjects that are no longer assigned
        for (ClassSubject cs : existing) {
            if (!incomingSubjectIds.contains(cs.getSubject().getId())) {

                if (cs.isLocked()) {
                    throw new RuleException(
                            "Subject '" + cs.getSubject().getName()
                                    + "' cannot be removed because student assessments already contain grades");
                }

                String subjectId = cs.getSubject().getId();

                // Remove teacher assignment
                teacherSubjectRepository.deleteByClassIdAndSubjectIdAndSchoolId(
                        classId,
                        subjectId,
                        schoolId
                );

                // Delete class subject
                repo.delete(cs);
            }
        }

        // Sync enrolled students for remaining and new subjects
        syncAssessmentsForEnrolledStudents(schoolId, classId, assignments, incomingSubjects, removedSubjectIds);
    }

    private void syncAssessmentsForEnrolledStudents(
            String schoolId,
            String classId,
            List<SubjectAssignment> assignments,
            Map<String, Subject> incomingSubjects,
            Set<String> removedSubjectIds) {

        var requiredSubjectIds = assignments.stream()
                .filter(assignment -> assignment.subjectGroupId() == null || assignment.subjectGroupId().isBlank())
                .map(SubjectAssignment::subjectId)
                .toList();

        if (requiredSubjectIds.isEmpty() && removedSubjectIds.isEmpty()) {
            return;
        }

        var enrollments = enrollmentRepo
                .findAllByClassAndSchoolId(classId, schoolId, Pageable.unpaged())
                .getContent();

        for (var enrollment : enrollments) {
            for (String removedSubjectId : removedSubjectIds) {
                studentAssessmentRepo.deleteByEnrollmentIdAndSubjectIdAndSchoolId(
                        enrollment.getId(), removedSubjectId, schoolId);
                enrollmentSubjectRepo.deleteByEnrollmentIdAndSubjectIdAndSchoolId(
                        enrollment.getId(), removedSubjectId, schoolId);
            }

            for (String subjectId : requiredSubjectIds) {
                if (enrollmentSubjectRepo.existsByEnrollmentIdAndSubjectIdAndSchoolId(
                        enrollment.getId(), subjectId, schoolId)) {
                    continue;
                }

                var subject = incomingSubjects.get(subjectId);
                if (subject == null) {
                    continue;
                }

                var enrollmentSubjectId = rg.generate("ENROLLMENT_SUBJECT", "ESB");
                enrollmentSubjectRepo.save(EnrollmentSubject.create(
                        enrollmentSubjectId,
                        schoolId,
                        enrollment,
                        subject,
                        enrollment.getStudent()));
            }

            provisionStudentAssessmentsUseCase.execute(enrollment);
        }
    }

    public record SubjectAssignment(String subjectId, String subjectGroupId, boolean core) {
    }

    private boolean hasClassSubjectChanged(ClassSubject classSubject, String nextGroupId, boolean nextMandatory) {
        String currentGroupId = classSubject.getGroup() == null ? "" : classSubject.getGroup().getId();
        String normalizedNextGroupId = nextGroupId == null ? "" : nextGroupId.trim();

        if (!currentGroupId.equals(normalizedNextGroupId)) {
            return true;
        }

        return !Boolean.valueOf(nextMandatory).equals(classSubject.getMandatory());
    }
}