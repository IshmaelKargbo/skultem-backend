package com.moriba.skultem.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.security.AuthUser;

class GradeAssessmentUseCaseTest {

    private static final String SCHOOL = "school-1";

    private final TeacherSubjectRepository teacherSubjectRepo = mock(TeacherSubjectRepository.class);
    private final AssessmentScoreRepository scoreRepo = mock(AssessmentScoreRepository.class);
    private final ClassSubjectRepository classSubjectRepo = mock(ClassSubjectRepository.class);
    private final StudentAssessmentRepository studentAssessmentRepo = mock(StudentAssessmentRepository.class);
    private final GradeAssessmentUseCase useCase = new GradeAssessmentUseCase(teacherSubjectRepo, scoreRepo,
            classSubjectRepo, studentAssessmentRepo);

    @BeforeEach
    void signIn() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser("user-1", SCHOOL, Role.ADMIN), null, List.of()));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    // A class with no stream (Nursery, Primary, JSS) - the session's stream is null.
    @Test
    void gradingWorksForAClassWithNoStream() {
        var subject = mock(Subject.class);
        when(subject.getId()).thenReturn("subj-1");
        var clazz = mock(Clazz.class);
        when(clazz.getId()).thenReturn("class-1");
        when(clazz.getLevel()).thenReturn(Level.NURSERY);
        var session = mock(ClassSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.getClazz()).thenReturn(clazz);
        when(session.getStream()).thenReturn(null);
        var ts = mock(TeacherSubject.class);
        when(ts.getSubject()).thenReturn(subject);
        when(ts.getSession()).thenReturn(session);
        when(teacherSubjectRepo.findByIdAndSchoolId("ts-1", SCHOOL)).thenReturn(Optional.of(ts));

        var classSubject = mock(ClassSubject.class);
        when(classSubjectRepo.findByClassIdAndSubjectId("class-1", "subj-1", SCHOOL))
                .thenReturn(Optional.of(classSubject));

        var sa = mock(StudentAssessment.class);
        when(sa.getId()).thenReturn("sa-1");
        when(studentAssessmentRepo.findAllBySubjectAndSessionAndTermId("subj-1", "session-1", "term-1"))
                .thenReturn(List.of(sa));

        var score = mock(AssessmentScore.class);
        when(score.getId()).thenReturn("score-1");
        when(score.canMark()).thenReturn(true);
        when(scoreRepo.findAllByStudentAssessmentIdAndAssessmentId("sa-1", "assess-1")).thenReturn(List.of(score));

        useCase.execute(SCHOOL, "ts-1", "assess-1", "term-1",
                List.of(new GradeAssessmentUseCase.Grade("score-1", 17)));

        verify(classSubject).lock();
        verify(score).updateScore(17, "user-1");
        verify(scoreRepo).saveAll(anyList());
    }
}
