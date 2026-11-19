package com.moriba.skultem.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.Role;

// The register, roster and grading endpoints are open to the TEACHER role; these checks are what keep a
// teacher to their own classes and subjects. Management is never limited by them.
@ExtendWith(MockitoExtension.class)
class TeacherOwnershipPermissionTest {

    private static final String SCHOOL = "school-1";

    @Mock SchoolUserRepository schoolUserRepo;
    @Mock TeacherRepository teacherRepo;
    @Mock ClassMasterRepository classMasterRepo;
    @Mock TeacherSubjectRepository teacherSubjectRepo;
    @InjectMocks PermissionService permissions;

    private final Teacher me = teacher("teacher-me", "user-me");
    private final Teacher other = teacher("teacher-other", "user-other");

    @BeforeEach
    void signIn() {
        actAs("user-me", Role.TEACHER, Set.of(Role.TEACHER));
        lenient().when(teacherRepo.findByUserIdAndSchoolId("user-me", SCHOOL)).thenReturn(Optional.of(me));
        lenient().when(teacherSubjectRepo.findByTeacherIdAndSchoolId(anyString(), eq(SCHOOL))).thenReturn(List.of());
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private void actAs(String userId, Role active, Set<Role> roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(userId, SCHOOL, active), null, List.of()));
        lenient().when(schoolUserRepo.findBySchoolAndUserAndRole(eq(SCHOOL), eq(userId), any())).thenAnswer(
                inv -> roles.contains(inv.getArgument(2, Role.class))
                        ? Optional.of(mock(SchoolUser.class))
                        : Optional.empty());
    }

    private static Teacher teacher(String id, String userId) {
        var user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);
        var t = mock(Teacher.class);
        lenient().when(t.getId()).thenReturn(id);
        lenient().when(t.getUser()).thenReturn(user);
        return t;
    }

    private static TeacherSubject subject(String id, Teacher teacher, String classId) {
        var clazz = mock(Clazz.class);
        lenient().when(clazz.getId()).thenReturn(classId);
        var session = mock(ClassSession.class);
        lenient().when(session.getClazz()).thenReturn(clazz);
        var ts = mock(TeacherSubject.class);
        lenient().when(ts.getTeacher()).thenReturn(teacher);
        lenient().when(ts.getSession()).thenReturn(session);
        return ts;
    }

    // ── Register ──────────────────────────────────────────────────────────────────────────────

    @Test
    void aClassMasterTakesTheirOwnRegister() {
        when(classMasterRepo.existsByTeacherIdAndClassSessionIdAndSchoolId("teacher-me", "sess-mine", SCHOOL))
                .thenReturn(true);
        assertThat(permissions.canTakeRegister(SCHOOL, "sess-mine")).isTrue();
    }

    @Test
    void anotherTeacherCannotTakeTheRegister() {
        when(classMasterRepo.existsByTeacherIdAndClassSessionIdAndSchoolId("teacher-me", "sess-theirs", SCHOOL))
                .thenReturn(false);
        assertThat(permissions.canTakeRegister(SCHOOL, "sess-theirs")).isFalse();
    }

    @Test
    void managementTakesAnyRegister() {
        actAs("user-admin", Role.ADMIN, Set.of(Role.ADMIN));
        assertThat(permissions.canTakeRegister(SCHOOL, "sess-any")).isTrue();
    }

    @Test
    void aStaffMemberWhoIsAlsoAdminIsNotLimited() {
        actAs("user-me", Role.TEACHER, Set.of(Role.TEACHER, Role.ADMIN));
        assertThat(permissions.canTakeRegister(SCHOOL, "sess-theirs")).isTrue();
        assertThat(permissions.canViewClassRoster(SCHOOL, "class-theirs")).isTrue();
    }

    // ── Class roster ──────────────────────────────────────────────────────────────────────────

    @Test
    void theClassMasterSeesTheRoster() {
        when(classMasterRepo.existsActiveByTeacherIdAndClassIdAndSchoolId("teacher-me", "class-mine", SCHOOL))
                .thenReturn(true);
        assertThat(permissions.canViewClassRoster(SCHOOL, "class-mine")).isTrue();
    }

    @Test
    void aSubjectTeacherOfTheClassSeesTheRoster() {
        var maths = subject("ts-maths", me, "class-taught");
        when(teacherSubjectRepo.findByTeacherIdAndSchoolId("teacher-me", SCHOOL)).thenReturn(List.of(maths));
        assertThat(permissions.canViewClassRoster(SCHOOL, "class-taught")).isTrue();
    }

    @Test
    void anUnrelatedTeacherCannotSeeTheRoster() {
        var maths = subject("ts-maths", me, "class-taught");
        when(teacherSubjectRepo.findByTeacherIdAndSchoolId("teacher-me", SCHOOL)).thenReturn(List.of(maths));
        assertThat(permissions.canViewClassRoster(SCHOOL, "class-theirs")).isFalse();
    }

    @Test
    void theOtherRolesTheEndpointNamesStillSeeTheRoster() {
        actAs("user-acc", Role.ACCOUNTANT, Set.of(Role.ACCOUNTANT));
        assertThat(permissions.canViewClassRoster(SCHOOL, "class-any", "ACCOUNTANT")).isTrue();
        assertThat(permissions.canViewClassRoster(SCHOOL, "class-any")).isFalse();
    }

    // ── Grading ───────────────────────────────────────────────────────────────────────────────

    @Test
    void aTeacherGradesTheirOwnSubject() {
        var mine = subject("ts-mine", me, "class-1");
        when(teacherSubjectRepo.findByIdAndSchoolId("ts-mine", SCHOOL)).thenReturn(Optional.of(mine));
        assertThat(permissions.ownsTeacherSubject(SCHOOL, "ts-mine")).isTrue();
    }

    @Test
    void aTeacherCannotGradeSomeoneElsesSubject() {
        var theirs = subject("ts-theirs", other, "class-1");
        when(teacherSubjectRepo.findByIdAndSchoolId("ts-theirs", SCHOOL)).thenReturn(Optional.of(theirs));
        assertThat(permissions.ownsTeacherSubject(SCHOOL, "ts-theirs")).isFalse();
    }

    @Test
    void theClassMasterCanReadButNotGradeAnotherTeachersScores() {
        var theirs = subject("ts-theirs", other, "class-mine");
        when(teacherSubjectRepo.findByIdAndSchoolId("ts-theirs", SCHOOL)).thenReturn(Optional.of(theirs));
        when(classMasterRepo.existsActiveByTeacherIdAndClassIdAndSchoolId("teacher-me", "class-mine", SCHOOL))
                .thenReturn(true);
        assertThat(permissions.canViewTeacherSubjectScores(SCHOOL, "ts-theirs")).isTrue();
        assertThat(permissions.ownsTeacherSubject(SCHOOL, "ts-theirs")).isFalse();
    }

    @Test
    void anUnknownSubjectIsRefused() {
        when(teacherSubjectRepo.findByIdAndSchoolId("ts-gone", SCHOOL)).thenReturn(Optional.empty());
        assertThat(permissions.ownsTeacherSubject(SCHOOL, "ts-gone")).isFalse();
    }
}
