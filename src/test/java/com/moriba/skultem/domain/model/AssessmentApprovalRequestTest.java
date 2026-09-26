package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

// Nobody approves their own grades: a class master who submitted the assessment (they teach the subject) hands
// the review to an admin, proprietor or owner.
class AssessmentApprovalRequestTest {

    private AssessmentApprovalRequest request(String masterTeacherId, String submitterTeacherId) {
        var masterTeacher = mock(Teacher.class);
        when(masterTeacher.getId()).thenReturn(masterTeacherId);
        var master = mock(ClassMaster.class);
        when(master.getTeacher()).thenReturn(masterTeacher);

        var submitter = mock(Teacher.class);
        when(submitter.getId()).thenReturn(submitterTeacherId);
        var subject = mock(TeacherSubject.class);
        when(subject.getTeacher()).thenReturn(submitter);

        var now = Instant.now();
        return new AssessmentApprovalRequest("r1", "school", master, null, subject, null, "note", null, null,
                AssessmentApprovalRequest.Status.PENDING_REVIEW, now, now);
    }

    @Test
    void aClassMasterWhoTeachesTheSubjectCannotReviewTheirOwnSubmission() {
        assertThat(request("t1", "t1").isSelfReview()).isTrue();
    }

    @Test
    void anotherTeachersSubmissionIsReviewedByTheClassMaster() {
        assertThat(request("t1", "t2").isSelfReview()).isFalse();
    }
}
