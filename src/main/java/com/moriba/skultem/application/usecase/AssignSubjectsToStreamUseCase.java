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
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.EnrollmentSubject;
import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignSubjectsToStreamUseCase {

        private final StreamRepository streamRepo;
        private final SubjectRepository subjectRepo;
        private final SubjectGroupRepository groupRepo;
        private final StreamSubjectRepository repo;
        private final EnrollmentRepository enrollmentRepo;
        private final AcademicYearRepository academicYearRepo;
        private final EnrollmentSubjectRepository enrollmentSubjectRepo;
        private final StudentAssessmentRepository studentAssessmentRepo;
        private final TeacherSubjectRepository teacherSubjectRepo;
        private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;
        private final ReferenceGeneratorUsecase rg;
        private final LogActivityUseCase logActivityUseCase;

        @AuditLogAnnotation(action = "ASSIGNED_SUBJECT_TO_STREAM")
        public void execute(String schoolId, String streamId, List<SubjectAssignment> assignments) {

                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new RuleException("Active academic year not found"));

                var stream = streamRepo.findByIdAndSchoolId(streamId, schoolId)
                                .orElseThrow(() -> new RuleException("Stream not found"));

                // Filter valid assignments
                assignments = assignments == null ? List.of()
                                : assignments.stream()
                                                .filter(a -> a.subjectId() != null && !a.subjectId().isBlank())
                                                .toList();

                var subjectIds = assignments.stream().map(SubjectAssignment::subjectId).toList();
                if (subjectIds.size() != subjectIds.stream().distinct().count()) {
                        throw new RuleException("Duplicate subjects detected in request.");
                }

                // Existing StreamSubjects
                var existing = repo.findAllByStreamIdAndSchoolId(streamId, schoolId, Pageable.unpaged())
                                .getContent();
                var existingMap = existing.stream()
                                .collect(Collectors.toMap(
                                                ss -> ss.getSubject().getId(),
                                                ss -> ss,
                                                (a, b) -> {
                                                        throw new RuleException(
                                                                        "Duplicate stream subject records exist. Clean up first.");
                                                }));

                // Incoming subjects map
                Map<String, Subject> incomingSubjects = new HashMap<>();
                for (var assignment : assignments) {
                        var subject = subjectRepo.findById(assignment.subjectId())
                                        .orElseThrow(() -> new RuleException(
                                                        "Subject not found: " + assignment.subjectId()));

                        if (!subject.getSchoolId().equals(schoolId)) {
                                throw new RuleException(
                                                "Subject '" + subject.getName() + "' does not belong to this school.");
                        }

                        incomingSubjects.put(subject.getId(), subject);
                }

                // Subject groups
                var groupIds = assignments.stream()
                                .map(SubjectAssignment::subjectGroupId)
                                .filter(id -> id != null && !id.isBlank())
                                .collect(Collectors.toSet());

                var groups = groupIds.isEmpty() ? List.<SubjectGroup>of()
                                : groupRepo.findAllByIdInAndStreamAndSchoolId(groupIds, streamId, schoolId);
                var groupMap = groups.stream().collect(Collectors.toMap(SubjectGroup::getId, g -> g));

                // Determine removed subjects
                Set<String> existingSubjectIds = existing.stream()
                                .map(ss -> ss.getSubject().getId())
                                .collect(Collectors.toCollection(HashSet::new));
                Set<String> incomingSubjectIds = incomingSubjects.keySet();
                Set<String> removedSubjectIds = new HashSet<>(existingSubjectIds);
                removedSubjectIds.removeAll(incomingSubjectIds);

                // Update existing and add new StreamSubjects
                for (var assignment : assignments) {
                        var subject = incomingSubjects.get(assignment.subjectId());

                        SubjectGroup group = null;
                        if (assignment.subjectGroupId() != null && !assignment.subjectGroupId().isBlank()) {
                                group = groupMap.get(assignment.subjectGroupId());
                                if (group == null) {
                                        throw new RuleException(
                                                        "Invalid subject group: " + assignment.subjectGroupId());
                                }
                        }

                        if (existingMap.containsKey(subject.getId())) {
                                var ss = existingMap.get(subject.getId());
                                ss.update(subject, group, assignment.mandatory());
                                repo.save(ss);
                        } else {
                                var record = StreamSubject.create(UUID.randomUUID().toString(), schoolId, stream,
                                                subject, group, assignment.mandatory());
                                repo.save(record);
                        }
                }

                // Delete removed StreamSubjects + TeacherSubject cleanup
                for (var ss : existing) {
                        if (!incomingSubjectIds.contains(ss.getSubject().getId())) {
                                repo.delete(ss);

                                // Delete teacher assignments for this removed subject
                                teacherSubjectRepo.deleteByStreamIdAndSubjectIdAndSchoolId(
                                                streamId, ss.getSubject().getId(), schoolId);

                                var enrollments = enrollmentRepo
                                                .findAllByStreamIdAndAcademicYearIdAndSchoolId(streamId,
                                                                academicYear.getId(), schoolId);
                                for (var enrollment : enrollments) {
                                        enrollmentSubjectRepo.deleteByEnrollmentIdAndSubjectIdAndSchoolId(
                                                        enrollment.getId(), ss.getSubject().getId(), schoolId);
                                        studentAssessmentRepo.deleteByEnrollmentIdAndSubjectIdAndSchoolId(
                                                        enrollment.getId(), ss.getSubject().getId(), schoolId);
                                }
                        }
                }

                syncAssessmentsForEnrolledStudents(schoolId, streamId, academicYear.getId(), assignments,
                                incomingSubjects, removedSubjectIds);

                String meta = "assignedCount=" + incomingSubjects.size() + ";removedCount=" + removedSubjectIds.size();
                logActivityUseCase.log(
                                schoolId,
                                ActivityType.SUBJECT,
                                "Subjects assigned to stream",
                                stream.getName(),
                                meta,
                                stream.getId());
        }

        private void syncAssessmentsForEnrolledStudents(
                        String schoolId,
                        String streamId,
                        String academicYearId,
                        List<SubjectAssignment> assignments,
                        Map<String, Subject> incomingSubjects,
                        Set<String> removedSubjectIds) {

                var requiredSubjectIds = assignments.stream()
                                .filter(a -> a.subjectGroupId() == null || a.subjectGroupId().isBlank())
                                .map(SubjectAssignment::subjectId)
                                .toList();

                if (requiredSubjectIds.isEmpty() && removedSubjectIds.isEmpty())
                        return;

                var enrollments = enrollmentRepo
                                .findAllByStreamIdAndAcademicYearIdAndSchoolId(streamId, academicYearId, schoolId);

                for (Enrollment enrollment : enrollments) {
                        // Remove old enrollment subjects & student assessments for removed subjects
                        for (String removedSubjectId : removedSubjectIds) {
                                enrollmentSubjectRepo.deleteByEnrollmentIdAndSubjectIdAndSchoolId(
                                                enrollment.getId(), removedSubjectId, schoolId);
                                studentAssessmentRepo.deleteByEnrollmentIdAndSubjectIdAndSchoolId(
                                                enrollment.getId(), removedSubjectId, schoolId);
                        }

                        // Add new enrollment subjects
                        for (String subjectId : requiredSubjectIds) {
                                if (enrollmentSubjectRepo.existsByEnrollmentIdAndSubjectIdAndSchoolId(
                                                enrollment.getId(), subjectId, schoolId)) {
                                        continue;
                                }

                                var subject = incomingSubjects.get(subjectId);
                                if (subject == null)
                                        continue;

                                var enrollmentSubjectId = rg.generate("ENROLLMENT_SUBJECT", "ESB");
                                enrollmentSubjectRepo.save(EnrollmentSubject.create(
                                                enrollmentSubjectId, schoolId, enrollment, subject,
                                                enrollment.getStudent()));
                        }

                        provisionStudentAssessmentsUseCase.execute(enrollment);
                }
        }

        public record SubjectAssignment(String subjectId, String subjectGroupId, boolean mandatory) {
        }
}
