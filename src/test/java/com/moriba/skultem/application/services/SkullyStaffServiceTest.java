package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.vo.Role;

// What each staff role may ask Skully about - the same roles as the fee and attention reports in the app.
class SkullyStaffServiceTest {

    @Test
    void leadersSeeEverything() {
        for (Role r : EnumSet.of(Role.OWNER, Role.PROPRIETOR, Role.PRINCIPAL, Role.SUPER_ADMIN)) {
            assertThat(SkullyStaffService.parts(EnumSet.of(r))).containsExactly("attendance", "academics", "fees");
        }
    }

    @Test
    void adminsAndExamOfficersSeeNoFees() {
        assertThat(SkullyStaffService.parts(EnumSet.of(Role.ADMIN))).containsExactly("attendance", "academics");
        assertThat(SkullyStaffService.parts(EnumSet.of(Role.EXAM_OFFICER))).containsExactly("attendance", "academics");
    }

    @Test
    void accountantsSeeOnlyFees() {
        assertThat(SkullyStaffService.parts(EnumSet.of(Role.ACCOUNTANT))).containsExactly("fees");
    }

    @Test
    void rolesAddUp() {
        assertThat(SkullyStaffService.parts(EnumSet.of(Role.ADMIN, Role.ACCOUNTANT)))
                .containsExactly("attendance", "academics", "fees");
    }

    @Test
    void teachersParentsAndOthersAreNotStaffOnSkully() {
        for (Role r : EnumSet.of(Role.TEACHER, Role.PARENT, Role.LIBRARIAN, Role.NURSE)) {
            assertThat(SkullyStaffService.STAFF_ROLES).doesNotContain(r);
            assertThat(SkullyStaffService.parts(EnumSet.of(r))).isEmpty();
        }
    }
}
