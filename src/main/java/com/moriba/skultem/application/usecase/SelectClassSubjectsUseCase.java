package com.moriba.skultem.application.usecase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.EnrollmentSubject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SelectClassSubjectsUseCase {

    private final EnrollmentRepository enrollmentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final SubjectGroupRepository subjectGroupRepo;
    private final EnrollmentSubjectRepository enrollmentSubjectRepo;
    private final ReferenceGeneratorUsecase referenceGenerator;

    public void execute(ClassSubjectSelection param) {

        String schoolId = param.schoolId();

        Enrollment enrollment = enrollmentRepo
                .findByIdAndSchoolId(param.enrollmentId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

        String academicYearId = enrollment.getAcademicYear().getId();

        var academicYear = academicYearRepo
                .findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (academicYear.isLocked()) {
            throw new RuleException("Academic year is locked");
        }

        List<ClassSubject> allSubjects = classSubjectRepo.findAllByClassIdAndSchoolId(
                enrollment.getClazz().getId(),
                schoolId, Pageable.unpaged()).getContent();

        if (allSubjects.isEmpty()) {
            throw new RuleException("No subjects configured for this class");
        }

        List<ClassSubject> requiredSubjects = allSubjects.stream()
                .filter(cs -> cs.getGroup() == null)
                .toList();

        List<ClassSubject> optionalSubjects = allSubjects.stream()
                .filter(cs -> cs.getGroup() != null)
                .toList();

        // Auto add required subjects
        for (ClassSubject cs : requiredSubjects) {

            String subjectId = cs.getSubject().getId();

            if (!enrollmentSubjectRepo.existsByEnrollmentIdAndSubjectIdAndSchoolId(
                    enrollment.getId(), subjectId, schoolId)) {

                String id = referenceGenerator.generate("ENROLLMENT_SUBJECT", "ESB");

                enrollmentSubjectRepo.save(
                        EnrollmentSubject.create(id, schoolId, enrollment, cs.getSubject(), enrollment.getStudent()));
            }
        }

        if (optionalSubjects.isEmpty()) {
            return;
        }

        Map<String, ClassSubject> optionalSubjectMap = optionalSubjects.stream()
                .collect(Collectors.toMap(
                        cs -> cs.getSubject().getId(),
                        cs -> cs));

        Map<String, List<ClassSubject>> subjectsByGroup = optionalSubjects.stream()
                .collect(Collectors.groupingBy(
                        cs -> cs.getGroup().getId()));

        Set<String> groupIds = subjectsByGroup.keySet();

        Map<String, SubjectGroup> groupMap = subjectGroupRepo.findAllByIdsAndSchoolId(groupIds, schoolId)
                .stream()
                .collect(Collectors.toMap(SubjectGroup::getId, g -> g));

        List<String> selectedIds = param.optionalSubjectIds() != null
                ? param.optionalSubjectIds().stream().distinct().toList()
                : List.of();

        Map<String, Long> selectionCountPerGroup = new HashMap<>();

        // Validate selection + count
        for (String subjectId : selectedIds) {

            ClassSubject cs = optionalSubjectMap.get(subjectId);

            if (cs == null) {
                throw new RuleException(
                        "Subject does not belong to this class: " + subjectId);
            }

            selectionCountPerGroup.merge(cs.getGroup().getId(), 1L, (a, b) -> a + b);
        }

        // Validate group min/max
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

        // Save selected optional subjects
        for (String subjectId : selectedIds) {

            if (!enrollmentSubjectRepo.existsByEnrollmentIdAndSubjectIdAndSchoolId(
                    enrollment.getId(), subjectId, schoolId)) {

                ClassSubject cs = optionalSubjectMap.get(subjectId);

                String id = referenceGenerator.generate("ENROLLMENT_SUBJECT", "ESB");

                enrollmentSubjectRepo.save(
                        EnrollmentSubject.create(
                                id,
                                schoolId,
                                enrollment,
                                cs.getSubject(),
                                enrollment.getStudent()));
            }
        }
    }

    public record ClassSubjectSelection(
            String schoolId,
            String enrollmentId,
            List<String> optionalSubjectIds) {
    }
}
