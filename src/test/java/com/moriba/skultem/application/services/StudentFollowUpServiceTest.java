package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.StudentFollowUp;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentFollowUpRepository;
import com.moriba.skultem.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StudentFollowUpServiceTest {

    @Mock StudentFollowUpRepository repo;
    @Mock EnrollmentRepository enrollmentRepo;
    @Mock UserRepository userRepo;
    @InjectMocks StudentFollowUpService service;

    @Test
    void recordsWhatWasDoneByWhom() {
        when(enrollmentRepo.findByClassAndStudentAndSchoolId("c1", "p1", "s1"))
                .thenReturn(Optional.of(mock(Enrollment.class)));
        var user = mock(User.class);
        when(user.getName()).thenReturn("Isatu Bangura");
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        var dto = service.record("s1", "c1", "p1", "CALLED_GUARDIAN", "  Mother says she was sick  ", false, "u1");

        var saved = ArgumentCaptor.forClass(StudentFollowUp.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getAction()).isEqualTo(StudentFollowUp.Action.CALLED_GUARDIAN);
        assertThat(saved.getValue().getNote()).isEqualTo("Mother says she was sick");
        assertThat(saved.getValue().getClassId()).isEqualTo("c1");
        assertThat(dto.recordedBy()).isEqualTo("Isatu Bangura");
        assertThat(dto.resolved()).isFalse();
    }

    @Test
    void onlyForAPupilOfThatClass() {
        when(enrollmentRepo.findByClassAndStudentAndSchoolId("c1", "p9", "s1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.record("s1", "c1", "p9", "MET_GUARDIAN", null, true, "u1"))
                .isInstanceOf(NotFoundException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void anUnknownActionIsRefused() {
        assertThatThrownBy(() -> service.record("s1", "c1", "p1", "SHOUTED", null, false, "u1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void otherNeedsANote() {
        assertThatThrownBy(() -> service.record("s1", "c1", "p1", "OTHER", "   ", false, "u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("note");
    }

    @Test
    void anOverlongNoteIsRefused() {
        assertThatThrownBy(() -> service.record("s1", "c1", "p1", "SPOKE_WITH_PUPIL", "x".repeat(1001), false, "u1"))
                .isInstanceOf(BadRequestException.class);
    }
}
