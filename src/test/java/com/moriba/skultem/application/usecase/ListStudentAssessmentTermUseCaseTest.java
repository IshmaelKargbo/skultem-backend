package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

// The grade-entry sheet lists the same students that saving and submitting work on: everyone taking the subject in
// the class section - not only rows stamped with this teacher's assignment, which left a replaced or second teacher
// with an empty or partial sheet that then failed to save ("Missing grade for score id").
@ExtendWith(MockitoExtension.class)
class ListStudentAssessmentTermUseCaseTest {

    @Mock StudentAssessmentRepository repo;
    @Mock TeacherSubjectRepository teacherSubjectRepo;
    @Mock AssessmentScoreRepository assessmentScoreRepo;
    @Mock AssessmentCaEntryRepository caEntryRepo;
    @Mock ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    @Mock AssessmentStructureService structureService;
    @InjectMocks ListStudentAssessmentTermUseCase useCase;

    @Test
    void readsTheWholeClassForTheSubjectNotJustThisTeachersRows() {
        var subject = mock(Subject.class);
        when(subject.getId()).thenReturn("sub-maths");
        var session = mock(ClassSession.class);
        when(session.getId()).thenReturn("sess-1");
        var ts = mock(TeacherSubject.class);
        when(ts.getSubject()).thenReturn(subject);
        when(ts.getSession()).thenReturn(session);
        when(teacherSubjectRepo.findByIdAndSchoolId("ts-new-teacher", "s1")).thenReturn(Optional.of(ts));
        when(repo.findAllBySubjectAndSessionAndTermId("sub-maths", "sess-1", "t1")).thenReturn(List.of());
        lenient().when(structureService.freezeOpened(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());

        assertThat(useCase.execute("s1", "ts-new-teacher", "t1")).isEmpty();

        verify(repo).findAllBySubjectAndSessionAndTermId("sub-maths", "sess-1", "t1");
        verify(repo, never()).findAllByTeacherSubjectIdTermId(anyString(), anyString());
    }

    @Test
    void anUnknownAssignmentListsNobody() {
        when(teacherSubjectRepo.findByIdAndSchoolId("gone", "s1")).thenReturn(Optional.empty());
        assertThat(useCase.execute("s1", "gone", "t1")).isEmpty();
        verify(repo, never()).findAllBySubjectAndSessionAndTermId(anyString(), anyString(), anyString());
    }
}
