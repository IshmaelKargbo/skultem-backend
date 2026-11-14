package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.moriba.skultem.application.usecase.LogActivityUseCase;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.FeatureModule;

// Skully gives a parent their portal sign-in; a temporary password only while the account was never used.
class SkullyPortalAccessTest {

    private final ParentRepository parents = mock(ParentRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final ModuleAccessService modules = mock(ModuleAccessService.class);
    private final LogActivityUseCase activity = mock(LogActivityUseCase.class);
    private final SkullyParentService service = new SkullyParentService(parents, null, schools, null, null, null,
            null, null, null, null, modules, activity, null, null, null, null);

    SkullyPortalAccessTest() {
        ReflectionTestUtils.setField(service, "defaultCountryCode", "232");
        ReflectionTestUtils.setField(service, "portalUrl", "https://{domain}.skultem.space");
        var school = mock(School.class);
        when(school.getId()).thenReturn("S1");
        when(school.getName()).thenReturn("Moriba Academic");
        when(school.getDomain()).thenReturn("moriba");
        when(schools.findById("S1")).thenReturn(Optional.of(school));
        when(modules.isEnabled("S1", FeatureModule.WHATSAPP_ASSISTANT)).thenReturn(true);
    }

    private void parentWith(User.Status status, String hint, String email) {
        var user = mock(User.class);
        when(user.getId()).thenReturn("U1");
        when(user.getStatus()).thenReturn(status);
        when(user.getHint()).thenReturn(hint);
        when(user.getEmail()).thenReturn(email);
        when(user.getGivenNames()).thenReturn("Mariama");
        when(user.getFamilyName()).thenReturn("Sesay");
        var parent = mock(Parent.class);
        when(parent.getSchoolId()).thenReturn("S1");
        when(parent.getPhone()).thenReturn("077123456");
        when(parent.getUser()).thenReturn(user);
        when(parents.findAllByPhoneDigits(anyCollection())).thenReturn(List.of(parent));
    }

    @Test
    void aPendingAccountGetsItsTemporaryPasswordAndTheSchoolSeesItWasSent() {
        parentWith(User.Status.RESET_PASSWORD, "Xy7#pQ2mRt9a", null);

        var access = service.portalAccess("23277123456").get(0);

        assertThat(access.status()).isEqualTo("PENDING");
        assertThat(access.temporaryPassword()).isEqualTo("Xy7#pQ2mRt9a");
        assertThat(access.loginUrl()).isEqualTo("https://moriba.skultem.space/login");
        assertThat(access.signInWith()).isEqualTo("077123456");
        verify(activity).log(eq("S1"), eq(ActivityType.USER), any(), eq("Mariama Sesay"), any(), eq("U1"));
    }

    @Test
    void anAccountInUseNeverGetsAPassword() {
        parentWith(User.Status.ACTIVE, "left-over", "mariama@example.com");

        var access = service.portalAccess("23277123456").get(0);

        assertThat(access.status()).isEqualTo("ACTIVE");
        assertThat(access.temporaryPassword()).isNull();
        assertThat(access.signInWith()).isEqualTo("mariama@example.com");
        verify(activity, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void nothingForASchoolWithoutSkully() {
        parentWith(User.Status.RESET_PASSWORD, "Xy7#pQ2mRt9a", null);
        when(modules.isEnabled("S1", FeatureModule.WHATSAPP_ASSISTANT)).thenReturn(false);

        assertThat(service.portalAccess("23277123456")).isEmpty();
    }
}
