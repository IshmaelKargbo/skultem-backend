package com.moriba.skultem.application.usecase;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Teacher.Status;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignSubjectToTeacherUseCase {

        private final TeacherRepository teacherRepo;
        private final ClassSessionRepository sessionRepo;
        private final AcademicYearRepository academicYearRepo;
        private final ClassSubjectRepository classSubjectRepo;
        private final StreamSubjectRepository streamSubjectRepo;
        private final SubjectRepository subjectRepo;
        private final TeacherSubjectRepository repo;
        private final ReferenceGeneratorUsecase referenceGenerator;

        public void execute(String schoolId, String sessionId, List<SubjectAssignment> assignments) {
                var academicYear = academicYearRepo
                                .findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Academic year not found"));

                if (academicYear.isLocked()) {
                        throw new RuleException("Cannot assign subjects in a locked academic year");
                }

                var session = sessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Class session not found"));

                if (assignments.stream().anyMatch(a -> a.teacherId() == null || a.subjectId() == null)) {
                        throw new RuleException("All assignments must have a teacherId and subjectId");
                }

                Map<String, Set<String>> teacherToSubjects = assignments.stream()
                                .collect(Collectors.groupingBy(
                                                SubjectAssignment::teacherId,
                                                Collectors.mapping(SubjectAssignment::subjectId, Collectors.toSet())));

                for (var entry : teacherToSubjects.entrySet()) {
                        String teacherId = entry.getKey();
                        Set<String> incomingSubjectIds = entry.getValue();

                        var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                                        .orElseThrow(() -> new NotFoundException("Teacher not found"));

                        if (teacher.getStatus() != Status.ACTIVE) {
                                throw new RuleException("Only active teachers can be assigned subjects");
                        }

                        List<TeacherSubject> existingAssignments = repo
                                        .findByTeacherIdAndClassSessionId(teacherId, sessionId, Pageable.unpaged())
                                        .getContent();
                        Set<String> existingSubjectIds = existingAssignments.stream()
                                        .map(ts -> ts.getSubject().getId())
                                        .collect(Collectors.toSet());

                        for (String subjectId : incomingSubjectIds) {
                                boolean subjectValid;
                                if (session.getStream() == null || session.getStream().getId() == null) {
                                        subjectValid = classSubjectRepo.existsByClassIdAndSubjectIdAndSchoolId(
                                                        session.getClazz().getId(), subjectId, schoolId);
                                } else {
                                        subjectValid = streamSubjectRepo.existsByStreamIdAndSubjectIdAndSchoolId(
                                                        session.getStream().getId(), subjectId, schoolId);
                                }
                                if (!subjectValid) {
                                        throw new RuleException(
                                                        "Subject does not belong to this class/stream: " + subjectId);
                                }

                                if (!existingSubjectIds.contains(subjectId)) {
                                        Subject subject = subjectRepo.findByIdAndSchoolId(subjectId, schoolId)
                                                        .orElseThrow(() -> new NotFoundException(
                                                                        "Subject not found: " + subjectId));

                                        String id = referenceGenerator.generate("TEACHER_SUBJECT", "TSB");
                                        var record = TeacherSubject.create(id, schoolId, session, teacher, subject);
                                        repo.save(record);
                                }
                        }

                        for (TeacherSubject existing : existingAssignments) {
                                String existingSubjectId = existing.getSubject().getId();
                                if (!incomingSubjectIds.contains(existingSubjectId)) {
                                        repo.delete(existing);
                                }
                        }
                }
        }

        public record SubjectAssignment(
                        String teacherId,
                        String subjectId) {
        }
}