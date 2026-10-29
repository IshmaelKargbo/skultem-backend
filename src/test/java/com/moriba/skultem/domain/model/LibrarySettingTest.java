package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;

// The rules the issue form and IssueLibraryBookUseCase both rely on.
class LibrarySettingTest {

    private final LibrarySetting defaults = LibrarySetting.defaults("school-1");

    @Test
    void defaultsAreTwoBooksForStudentsFiveForTeachersAndOverdueBlocks() {
        assertThat(defaults.maxLoansFor(BorrowerType.STUDENT)).isEqualTo(2);
        assertThat(defaults.maxLoansFor(BorrowerType.TEACHER)).isEqualTo(5);
        assertThat(defaults.isBlockWhenOverdue()).isTrue();
        assertThat(defaults.getDefaultLoanDays()).isEqualTo(14);
    }

    @Test
    void aBorrowerBelowTheLimitWithNothingOverdueCanBorrow() {
        assertThat(defaults.blockReason(BorrowerType.STUDENT, 1, 0)).isNull();
        assertThat(defaults.blockReason(BorrowerType.TEACHER, 4, 0)).isNull();
    }

    @Test
    void reachingTheLimitBlocks() {
        assertThat(defaults.blockReason(BorrowerType.STUDENT, 2, 0)).contains("2 of 2");
        assertThat(defaults.blockReason(BorrowerType.TEACHER, 5, 0)).contains("5 of 5");
    }

    @Test
    void anOverdueBookBlocksEvenBelowTheLimit() {
        assertThat(defaults.blockReason(BorrowerType.TEACHER, 1, 1)).contains("overdue");
    }

    @Test
    void theOverdueRuleCanBeSwitchedOff() {
        var setting = LibrarySetting.defaults("school-1");
        setting.update(3, 6, false, 7);

        assertThat(setting.blockReason(BorrowerType.STUDENT, 1, 1)).isNull();
        assertThat(setting.blockReason(BorrowerType.STUDENT, 3, 1)).contains("3 of 3");
    }

    @Test
    void limitsOutsideTheAllowedRangeAreRejected() {
        var setting = LibrarySetting.defaults("school-1");

        assertThatThrownBy(() -> setting.update(0, 5, true, 14)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setting.update(2, 51, true, 14)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setting.update(2, 5, true, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
