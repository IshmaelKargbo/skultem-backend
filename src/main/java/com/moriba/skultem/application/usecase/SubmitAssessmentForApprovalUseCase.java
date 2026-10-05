package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SubmitAssessmentForApprovalUseCase {

        private final AssessmentApprovalRequestRepository approvalRepo;
        private final TeacherSubjectRepository teacherSubjectRepo;
        private final AssessmentScoreRepository assessmentScoreRepo;
        private final StudentAssessmentRepository studentAssessmentRepo;
        private final ClassSubjectAssessmentLifeCycleRepository assessmentLifeCycleRepo;
        private final ClassMasterRepository classMasterRepo;
        private final com.moriba.skultem.domain.repository.SchoolRepository schoolRepo;
        private final com.moriba.skultem.application.services.GradeApprovalResolver gradeApprovalResolver;

        @AuditLogAnnotation(action = "ASSESSMENT_SUBMITED")
        public void execute(
                        String schoolId,
                        String teacherSubjectId,
                        String assessmentId,
                        String termId,
                        String note) {

                var teacherSubject = teacherSubjectRepo
                                .findByIdAndSchoolId(teacherSubjectId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Teacher subject not found"));

                // Who approves depends on the school (or the class's section): the class master, or an admin.
                // Only the class-master route needs one to exist - where an admin approves, a class with no
                // class master can still submit.
                var school = schoolRepo.findById(schoolId)
                                .orElseThrow(() -> new NotFoundException("School not found"));
                var approver = gradeApprovalResolver.forLevel(school,
                                teacherSubject.getSession().getClazz().getLevel());

                var classMasterLookup = classMasterRepo
                                .findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(
                                                teacherSubject.getSession().getId());
                if (approver == com.moriba.skultem.domain.vo.GradeApprover.CLASS_MASTER && classMasterLookup.isEmpty()) {
                        throw new NotFoundException("Class master not found");
                }
                var classMaster = classMasterLookup.orElse(null);

                var cycle = assessmentLifeCycleRepo
                                .findBySubjectSessionAssessmentAndTerm(
                                                teacherSubject.getSubject().getId(),
                                                teacherSubject.getSession().getId(),
                                                assessmentId,
                                                termId)
                                .orElseThrow(() -> new NotFoundException("Assessment cycle not found"));

                var studentAssessments = studentAssessmentRepo
                                .findAllBySubjectAndSessionAndTermId(teacherSubject.getSubject().getId(),
                                                teacherSubject.getSession().getId(), termId);

                if (studentAssessments.isEmpty()) {
                        throw new NotFoundException("No student assessments found");
                }

                for (var sa : studentAssessments) {

                        var scores = assessmentScoreRepo
                                        .findAllByStudentAssessmentIdAndAssessmentId(sa.getId(), assessmentId);

                        if (scores.isEmpty()) {
                                throw new RuleException("Assessment scores are missing");
                        }

                        scores.forEach(score -> {
                                if (!score.getCycle().canEdit()) {
                                        throw new RuleException(
                                                        "Assessment cannot be edited in the current state");
                                }
                                // Continuous assessment goes in two steps - the CA first, then the formal test -
                                // and both must be in before it goes for approval.
                                if (score.getCycle().isContinuous()) {
                                        // A monitor-only CA never has to be finished before approval.
                                        if (!score.getCycle().isCaSubmitted() && !score.getCycle().isMonitorOnly()) {
                                                throw new RuleException(
                                                                "Submit the CA recordings and enter the formal test before submitting for approval");
                                        }
                                        if (score.getFormalScore() == null) {
                                                throw new RuleException(
                                                                "The formal test is still missing for at least one student");
                                        }
                                }
                        });
                }

                var assessmentRes = approvalRepo.findByCycle(cycle.getId());
                AssessmentApprovalRequest approvalRequest;

                if (assessmentRes.isEmpty()) {
                        approvalRequest = AssessmentApprovalRequest.create(
                                        UUID.randomUUID().toString(),
                                        schoolId,
                                        classMaster,
                                        cycle,
                                        teacherSubject,
                                        cycle.getTerm(),
                                        note);
                } else {
                        approvalRequest = assessmentRes.get();
                        approvalRequest.resubmit(note);
                }

                cycle.submit();
                approvalRepo.save(approvalRequest);
                assessmentLifeCycleRepo.save(cycle);
        }
}
