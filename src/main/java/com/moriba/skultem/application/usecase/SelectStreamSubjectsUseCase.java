package com.moriba.skultem.application.usecase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.EnrollmentSubject;
import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SelectStreamSubjectsUseCase {

        private final EnrollmentRepository enrollmentRepo;
        private final AcademicYearRepository academicYearRepo;
        private final StreamSubjectRepository streamSubjectRepo;
        private final SubjectGroupRepository subjectGroupRepo;
        private final EnrollmentSubjectRepository repo;
        private final ReferenceGeneratorUsecase referenceGenerator;

        public void execute(StreamSubjectSelection param) {

                Enrollment enrollment = enrollmentRepo
                                .findByIdAndSchoolId(param.enrollmentId(), param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

                if (enrollment.getClazz().getLevel() != Level.SSS) {
                        throw new RuleException("Only SSS students can select stream subjects");
                }

                if (enrollment.getStream() == null) {
                        throw new RuleException("Student stream not assigned");
                }

                var academicYear = academicYearRepo
                                .findByIdAndSchoolId(
                                                enrollment.getAcademicYear().getId(),
                                                param.schoolId())
                                .orElseThrow(() -> new NotFoundException("Academic year not found"));

                if (academicYear.isLocked()) {
                        throw new RuleException("Academic year is locked");
                }

                List<StreamSubject> allStreamSubjects = streamSubjectRepo.findAllByStreamIdAndSchoolId(
                                enrollment.getStream().getId(),
                                param.schoolId(), Pageable.unpaged())
                                .getContent();

                if (allStreamSubjects.isEmpty()) {
                        throw new RuleException("No subjects configured for this stream");
                }

                // Split required & optional
                List<StreamSubject> requiredSubjects = allStreamSubjects.stream()
                                .filter(ss -> ss.getGroup() == null)
                                .toList();

                List<StreamSubject> optionalSubjects = allStreamSubjects.stream()
                                .filter(ss -> ss.getGroup() != null)
                                .toList();

                // Fetch existing enrollment subjects ONCE
                List<EnrollmentSubject> existingSubjects = repo.findAllByEnrollmentIdAndSchoolId(
                                enrollment.getId(),
                                param.schoolId());

                Set<String> existingSubjectIds = existingSubjects.stream()
                                .map(es -> es.getSubject().getId())
                                .collect(Collectors.toSet());

                for (StreamSubject ss : requiredSubjects) {

                        String subjectId = ss.getSubject().getId();

                        if (!existingSubjectIds.contains(subjectId)) {

                                String id = referenceGenerator.generate("ENROLLMENT_SUBJECT", "ESB");

                                repo.save(EnrollmentSubject.create(id, param.schoolId(), enrollment,
                                                ss.getSubject(), enrollment.getStudent()));

                                existingSubjectIds.add(subjectId);
                        }
                }

                if (optionalSubjects.isEmpty()) {
                        return;
                }

                List<String> selectedIds = param.optionalSubjectIds() != null
                                ? param.optionalSubjectIds().stream().distinct().toList()
                                : List.of();

                Map<String, StreamSubject> optionalSubjectMap = optionalSubjects.stream()
                                .collect(Collectors.toMap(
                                                ss -> ss.getSubject().getId(),
                                                ss -> ss));

                // Validate selected subjects belong to stream
                for (String subjectId : selectedIds) {
                        if (!optionalSubjectMap.containsKey(subjectId)) {
                                throw new RuleException(
                                                "Subject does not belong to this stream: " + subjectId);
                        }
                }

                Map<String, List<StreamSubject>> subjectsByGroup = optionalSubjects.stream()
                                .collect(Collectors.groupingBy(
                                                ss -> ss.getGroup().getId()));

                Set<String> groupIds = subjectsByGroup.keySet();

                Map<String, SubjectGroup> groupMap = subjectGroupRepo
                                .findAllByIdsAndSchoolId(groupIds, param.schoolId())
                                .stream()
                                .collect(Collectors.toMap(SubjectGroup::getId, g -> g));

                Map<String, Long> selectionCountPerGroup = new HashMap<>();

                for (String subjectId : selectedIds) {
                        StreamSubject ss = optionalSubjectMap.get(subjectId);
                        selectionCountPerGroup.merge(ss.getGroup().getId(), 1L, (a, b) -> a + b);
                }

                for (var entry : subjectsByGroup.entrySet()) {

                        String groupId = entry.getKey();
                        SubjectGroup group = groupMap.get(groupId);

                        if (group == null) {
                                throw new RuleException("Subject group not found: " + groupId);
                        }

                        long selectedCount = selectionCountPerGroup.getOrDefault(groupId, 0L);

                        if (selectedCount < group.getMinSelection()) {
                                throw new RuleException(
                                                "Minimum selection not met for group: " + group.getName());
                        }

                        if (selectedCount > group.getMaxSelection()) {
                                throw new RuleException(
                                                "Maximum selection exceeded for group: " + group.getName());
                        }
                }

                Set<String> newOptionalSet = new HashSet<>(selectedIds);

                existingSubjects.stream()
                                .filter(es -> optionalSubjectMap.containsKey(es.getSubject().getId()))
                                .filter(es -> !newOptionalSet.contains(es.getSubject().getId()))
                                .forEach(repo::delete);

                for (String subjectId : selectedIds) {

                        if (!existingSubjectIds.contains(subjectId)) {

                                StreamSubject ss = optionalSubjectMap.get(subjectId);

                                String id = referenceGenerator.generate("ENROLLMENT_SUBJECT", "ESB");

                                repo.save(EnrollmentSubject.create(id, param.schoolId(), enrollment, ss.getSubject(),
                                                enrollment.getStudent()));
                        }
                }
        }

        public record StreamSubjectSelection(
                        String schoolId,
                        String enrollmentId,
                        List<String> optionalSubjectIds) {
        }
}
