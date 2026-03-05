package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProvisionStudentAssessmentsUseCase {
    private final AssessmentRepository assessmentRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final ClassSessionRepository classSessionRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final EnrollmentSubjectRepository enrollmentSubjectRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final StreamSubjectRepository streamSubjectRepo;
    private final TermRepository termRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;

    public void execute(Enrollment enrollment) {
        if (enrollment == null || enrollment.getClazz() == null || enrollment.getClazz().getTemplate() == null) {
            return;
        }

        String schoolId = enrollment.getSchoolId();
        String templateId = enrollment.getClazz().getTemplate().getId();

        var session = classSessionRepo
                .findByAcademicYearAndClassAndSchoolId(enrollment.getAcademicYear().getId(),
                        enrollment.getClazz().getId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        var assessments = assessmentRepo.findAllByTemplateIdAndSchoolId(templateId, schoolId);
        if (assessments.isEmpty()) {
            throw new RuleException("Class has an assessment template with no assignments");
        }

        List<Term> terms = resolveTerms(enrollment);
        List<Subject> subjects = resolveSubjects(enrollment);

        List<StudentAssessment> studentAssessmentsToSave = new ArrayList<>();
        List<AssessmentScore> scoresToSave = new ArrayList<>();
        List<ClassSubjectAssessmentLifeCycle> cyclesToSave = new ArrayList<>();

        for (var term : terms) {
            for (var subject : subjects) {

                var teacherSubjectRes = teacherSubjectRepo
                        .findBySubjectIdAndSessionIdAndSchoolId(
                                subject.getId(),
                                session.getId(),
                                schoolId);

                if (teacherSubjectRes.isEmpty()) {
                    continue;
                }

                var teacherSubject = teacherSubjectRes.get();

                boolean saExists = studentAssessmentRepo.existsByEnrollmentIdAndTermIdAndSubjectIdAndSchoolId(
                        enrollment.getId(),
                        term.getId(),
                        subject.getId(),
                        enrollment.getSchoolId());

                if (saExists) {
                    continue;
                }

                var cycles = new ArrayList<>(cycleRepo.findAllBySubjectAndTerm(teacherSubject.getId(), term.getId()));
                if (cycles.isEmpty()) {
                    for (var assessment : assessments) {
                        var status = (term.getTermNumber() == 1 && assessment.getPosition() == 1)
                                ? ClassSubjectAssessmentLifeCycle.Status.DRAFT
                                : ClassSubjectAssessmentLifeCycle.Status.LOCKED;
                        var cycle = ClassSubjectAssessmentLifeCycle.create(
                                UUID.randomUUID().toString(),
                                schoolId,
                                teacherSubject,
                                term,
                                assessment,
                                status);
                        cyclesToSave.add(cycle);
                        cycles.add(cycle);
                    }
                }

                var sa = StudentAssessment.create(
                        UUID.randomUUID().toString(),
                        schoolId,
                        enrollment,
                        teacherSubject,
                        term);

                studentAssessmentsToSave.add(sa);

                for (var cycle : cycles) {
                    var score = AssessmentScore.create(
                            UUID.randomUUID().toString(),
                            schoolId,
                            sa,
                            cycle,
                            cycle.getAssessment().getWeight());

                    scoresToSave.add(score);
                }
            }
        }

        if (!cyclesToSave.isEmpty()) {
            cycleRepo.saveAll(cyclesToSave);
        }

        if (!studentAssessmentsToSave.isEmpty()) {
            studentAssessmentRepo.saveAll(studentAssessmentsToSave);
        }

        if (!scoresToSave.isEmpty()) {
            assessmentScoreRepo.saveAll(scoresToSave);
        }
    }

    private List<Term> resolveTerms(Enrollment enrollment) {
        var terms = termRepo.findByAcademicYearIdAndSchool(enrollment.getAcademicYear().getId(),
                enrollment.getSchoolId());

        if (terms.isEmpty()) {
            throw new NotFoundException("No terms found for this enrollment");
        }

        return terms.stream()
                .sorted(Comparator.comparing(Term::getTermNumber))
                .toList();
    }

    private List<Subject> resolveSubjects(Enrollment enrollment) {
        var fromEnrollment = enrollmentSubjectRepo
                .findAllByEnrollmentIdAndSchoolId(enrollment.getId(), enrollment.getSchoolId())
                .stream()
                .map(record -> record.getSubject())
                .filter(subject -> subject != null)
                .collect(Collectors.toMap(Subject::getId, subject -> subject, (a, b) -> a))
                .values()
                .stream()
                .toList();

        if (!fromEnrollment.isEmpty()) {
            return fromEnrollment;
        }

        if (enrollment.getStream() != null) {
            var streamSubjects = streamSubjectRepo
                    .findAllByStreamIdAndSchoolId(enrollment.getStream().getId(), enrollment.getSchoolId(),
                            Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(item -> Boolean.TRUE.equals(item.getMandatory()))
                    .map(item -> item.getSubject())
                    .collect(Collectors.toMap(Subject::getId, subject -> subject, (a, b) -> a))
                    .values()
                    .stream()
                    .toList();

            if (!streamSubjects.isEmpty()) {
                return streamSubjects;
            }
        }

        var classSubjects = classSubjectRepo
                .findAllByClassIdAndSchoolId(enrollment.getClazz().getId(), enrollment.getSchoolId(),
                        Pageable.unpaged())
                .getContent()
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getMandatory()))
                .map(item -> item.getSubject())
                .collect(Collectors.toMap(Subject::getId, subject -> subject, (a, b) -> a))
                .values()
                .stream()
                .toList();

        if (classSubjects.isEmpty()) {
            throw new RuleException("No subjects found for enrollment to provision student assessments");
        }

        return classSubjects;
    }
}
