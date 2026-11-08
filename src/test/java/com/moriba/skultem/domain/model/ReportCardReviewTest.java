package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ReportCard.ReviewStatus;

// Admin generates -> class teacher comments and approves (or sends back) -> final.
class ReportCardReviewTest {

    private ReportCard card() {
        return ReportCard.generate("c1", "s", "stu", "Aminata Kamara", "ADM-1", null, "jss1", "JSS 1", 30, "t1",
                "First Term", "2026/27", 72.5, 4, "B", true, 95.0, null, "[]", "scope", "Whole term", "admin-1");
    }

    @Test
    void aNewCardWaitsForTheClassTeacher() {
        assertThat(card().getReviewStatus()).isEqualTo(ReviewStatus.PENDING_REVIEW);
        assertThat(card().isApproved()).isFalse();
    }

    @Test
    void theClassTeacherCommentsThenApprovesAndTheCommentLocks() {
        var c = card();
        c.updateRemark("A hard-working student.");
        c.approve("Mr Kamara");
        assertThat(c.isApproved()).isTrue();
        assertThat(c.getReviewedByName()).isEqualTo("Mr Kamara");
        assertThatThrownBy(() -> c.updateRemark("Changed")).isInstanceOf(RuleException.class);
    }

    @Test
    void sendingBackKeepsTheNoteAndGeneratingAgainReopensTheReview() {
        var c = card();
        c.returnForChanges("Maths score for this student looks wrong", "Mr Kamara");
        assertThat(c.getReviewStatus()).isEqualTo(ReviewStatus.RETURNED);
        assertThat(c.getReviewNote()).contains("Maths");

        c.regenerate("Aminata Kamara", "ADM-1", null, "JSS 1", 30, "First Term", "2026/27", 75.0, 3, "B", true,
                95.0, "[]", "Whole term", "admin-1");
        assertThat(c.getReviewStatus()).isEqualTo(ReviewStatus.PENDING_REVIEW);
        assertThat(c.getReviewNote()).isNull();
    }

    @Test
    void anApprovedCardCantBeSentBack() {
        var c = card();
        c.approve("Mr Kamara");
        assertThatThrownBy(() -> c.returnForChanges("late", "Mr Kamara")).isInstanceOf(RuleException.class);
    }
}
