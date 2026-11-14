package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.moriba.skultem.domain.vo.FeatureModule;
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
    void teachersSeeOnlyTheirOwnClasses() {
        assertThat(SkullyStaffService.parts(EnumSet.of(Role.TEACHER))).containsExactly("myClasses");
        // A principal who also teaches gets both.
        assertThat(SkullyStaffService.parts(EnumSet.of(Role.PRINCIPAL, Role.TEACHER)))
                .containsExactly("attendance", "academics", "fees", "myClasses");
    }

    @Test
    void parentsAndOthersAreNotStaffOnSkully() {
        for (Role r : EnumSet.of(Role.PARENT, Role.LIBRARIAN, Role.NURSE)) {
            assertThat(SkullyStaffService.STAFF_ROLES).doesNotContain(r);
            assertThat(SkullyStaffService.parts(EnumSet.of(r))).isEmpty();
        }
    }

    // ---- who a number is (memberships): the whole number, never a part of it, and never two people

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private SkullyStaffService service(List<Map<String, Object>> rows) {
        var numbers = new SkullyReminderService(null, null, null, null, null);
        ReflectionTestUtils.setField(numbers, "countryCode", "232");
        var modules = mock(ModuleAccessService.class);
        when(modules.isEnabled(anyString(), any(FeatureModule.class))).thenReturn(true);
        when(jdbc.queryForList(anyString(), (Object) any(), (Object) any())).thenReturn(rows);
        return new SkullyStaffService(jdbc, modules, numbers, null, null);
    }

    private static Map<String, Object> row(String userId, String userPhone, String teacherPhone, String role) {
        Map<String, Object> r = new HashMap<>();
        r.put("id", userId);
        r.put("given_name", "Ishmael");
        r.put("family_name", "Kargbo");
        r.put("user_phone", userPhone);
        r.put("teacher_phone", teacherPhone);
        r.put("school_id", "S1");
        r.put("role", role);
        r.put("school_name", "Moriba Academic");
        return r;
    }

    @Test
    void aTeacherIsRecognisedByTheirTeacherProfilePhone() {
        var m = service(List.of(row("u1", null, "076 380 190", "TEACHER"))).memberships("23276380190");
        assertThat(m).hasSize(1);
    }

    @Test
    void theSameLastDigitsInAnotherCountryAreNotThem() {
        var m = service(List.of(row("u1", null, "+44 76380190", "TEACHER"))).memberships("23276380190");
        assertThat(m).isEmpty();
    }

    @Test
    void aNumberSavedOnAnAccountWinsOverSomeoneElsesTeacherProfile() {
        var m = service(List.of(row("u1", "23276380190", null, "ADMIN"), row("u2", null, "076380190", "TEACHER")))
                .memberships("23276380190");
        assertThat(m).extracting("userId").containsOnly("u1");
    }

    @Test
    void aNumberThatMatchesTwoPeopleGivesNoStaffAccess() {
        var m = service(List.of(row("u1", null, "076380190", "TEACHER"), row("u2", null, "+232 76 380 190", "TEACHER")))
                .memberships("23276380190");
        assertThat(m).isEmpty();
    }
}
