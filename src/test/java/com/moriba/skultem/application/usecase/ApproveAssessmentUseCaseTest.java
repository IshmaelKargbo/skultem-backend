package com.moriba.skultem.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;

class ApproveAssessmentUseCaseTest {

    private static final String SCHOOL = "school-1";

    private final AssessmentApprovalRequestRepository approvalRepo = mock(AssessmentApprovalRequestRepository.class);
    private final AssessmentScoreRepository scoreRepo = mock(AssessmentScoreRepository.class);
    private final StudentParentRepository studentParentRepo = mock(StudentParentRepository.class);
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo = mock(
            ClassSubjectAssessmentLifeCycleRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ApproveAssessmentUseCase useCase = new ApproveAssessmentUseCase(approvalRepo, scoreRepo,
            studentParentRepo, cycleRepo, publisher);

    // A student with no linked parent account has nobody to notify - approving the grades must
    // still go through instead of failing with "no parent relation found".
    @Test
    void approvesEvenWhenAStudentHasNoLinkedParent() {
        var ts = mock(TeacherSubject.class);
        when(ts.getId()).thenReturn("ts-1");
        var subject = mock(Subject.class);
        when(subject.getName()).thenReturn("Maths");
        when(ts.getSubject()).thenReturn(subject);
        var teacher = mock(Teacher.class);
        when(teacher.getName()).thenReturn("T");
        when(ts.getTeacher()).thenReturn(teacher);

        var assessment = mock(Assessment.class);
        when(assessment.getId()).thenReturn("a-1");
        when(assessment.getName()).thenReturn("Test");
        var term = mock(Term.class);
        when(term.getId()).thenReturn("term-1");
        when(term.getName()).thenReturn("Term 1");
        var cycle = mock(ClassSubjectAssessmentLifeCycle.class);
        when(cycle.getId()).thenReturn("cycle-1");
        when(cycle.getAssessment()).thenReturn(assessment);
        when(cycle.getTerm()).thenReturn(term);

        var request = mock(AssessmentApprovalRequest.class);
        when(request.isPending()).thenReturn(true);
        when(request.getTeacherSubject()).thenReturn(ts);
        when(request.getCycle()).thenReturn(cycle);
        when(request.getTerm()).thenReturn(term);
        when(approvalRepo.findByIdAndSchoolId("req-1", SCHOOL)).thenReturn(Optional.of(request));
        when(cycleRepo.findByTeacherSubjectAndAssessmentAndTerm("ts-1", "a-1", "term-1"))
                .thenReturn(Optional.of(cycle));

        var student = mock(Student.class);
        when(student.getId()).thenReturn("stu-1");
        when(student.getName()).thenReturn("Stu");
        var clazz = mock(Clazz.class);
        when(clazz.getName()).thenReturn("Nursery 1");
        var enrollment = mock(Enrollment.class);
        when(enrollment.getStudent()).thenReturn(student);
        when(enrollment.getClazz()).thenReturn(clazz);
        var sa = mock(StudentAssessment.class);
        when(sa.getEnrollment()).thenReturn(enrollment);
        when(sa.getTeacherSubject()).thenReturn(ts);
        var score = mock(AssessmentScore.class);
        when(score.getStudentAssessment()).thenReturn(sa);
        when(score.getScore()).thenReturn(70);
        when(score.getWeight()).thenReturn(50);
        when(score.getWeightedScore()).thenReturn(35);
        when(score.getAssessment()).thenReturn(assessment);
        when(scoreRepo.findAllByCycle("cycle-1")).thenReturn(List.of(score));
        when(studentParentRepo.findAllByStudentAndSchool("stu-1", SCHOOL)).thenReturn(List.of());

        useCase.execute(SCHOOL, "req-1", "ok");

        verify(cycle).approve();
        verify(request).approve("ok");
        verify(approvalRepo).save(request);
    }
}
