package com.moriba.skultem.application.usecase;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Teacher.Status;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

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
    private final EnrollmentRepository enrollmentRepo;
    private final ReferenceGeneratorUsecase referenceGenerator;
    private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ASSIGNED_SUBJECT_TO_TEACHER")
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
            throw new RuleException("TeacherId and SubjectId are required");
        }

        List<TeacherSubject> existingList = repo
                .findByClassSessionIdAndSchoolId(sessionId, schoolId, Pageable.unpaged())
                .getContent();

        Map<String, TeacherSubject> existingById = existingList.stream()
                .collect(Collectors.toMap(TeacherSubject::getId, ts -> ts));

        Set<String> incomingIds = new HashSet<>();

        for (SubjectAssignment assignment : assignments) {

            boolean subjectValid;
            if (session.getStream() == null) {
                subjectValid = classSubjectRepo.existsByClassIdAndSubjectIdAndSchoolId(
                        session.getClazz().getId(),
                        assignment.subjectId(),
                        schoolId);
            } else {
                subjectValid = streamSubjectRepo.existsByStreamIdAndSubjectIdAndSchoolId(
                        session.getStream().getId(),
                        assignment.subjectId(),
                        schoolId);
            }

            if (!subjectValid) {
                var id = assignment.subjectId();
                throw new RuleException("Subject does not belong to this class/stream: " + id);
            }

            var teacher = teacherRepo.findByIdAndSchoolId(assignment.teacherId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("Teacher not found"));

            if (teacher.getStatus() != Status.ACTIVE) {
                throw new RuleException("Only active teachers can be assigned");
            }

            TeacherSubject teacherSubject;
            if (assignment.id() != null) {
                var existing = existingById.get(assignment.id());
                if (existing == null) {
                    throw new NotFoundException("TeacherSubject not found: " + assignment.id());
                }

                existing.changeTeacher(teacher);
                repo.save(existing);
                incomingIds.add(existing.getId());
                teacherSubject = existing;
            } else {
                var subject = subjectRepo.findByIdAndSchoolId(assignment.subjectId(), schoolId)
                        .orElseThrow(() -> new NotFoundException("Subject not found"));

                String id = referenceGenerator.generate("TEACHER_SUBJECT", "TSB");
                teacherSubject = TeacherSubject.create(id, schoolId, session, teacher, subject);
                repo.save(teacherSubject);
                incomingIds.add(id);
            }

            var enrollments = enrollmentRepo.findAllByClassAndSchoolId(
                    session.getClazz().getId(),
                    schoolId,
                    Pageable.unpaged()).getContent();

            for (Enrollment enrollment : enrollments) {
                try {
                    provisionStudentAssessmentsUseCase.execute(enrollment);
                } catch (Exception e) {
                    continue;
                }
            }
        }

        for (TeacherSubject existing : existingList) {
            if (!incomingIds.contains(existing.getId())) {
                repo.delete(existing);
            }
        }

        String streamName = session.getStream() != null ? session.getStream().getName() : "No stream";
        String meta = "assignedCount=" + incomingIds.size();
        logActivityUseCase.log(
                schoolId,
                ActivityType.SUBJECT,
                "Subjects assigned to teachers",
                session.getClazz().getName() + " - " + session.getSection().getName() + " - " + streamName,
                meta,
                session.getId());
    }

    public record SubjectAssignment(String id, String teacherId, String subjectId) {
    }
}
