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
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.FeatureModule;

// Skully only answers for schools that installed the WhatsApp Assistant module.
class SkullyModuleGateTest {

    private final ParentRepository parents = mock(ParentRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final ModuleAccessService modules = mock(ModuleAccessService.class);
    private final SkullyParentService service = new SkullyParentService(parents, students, schools, null, null, null,
            null, null, null, null, modules, null);

    SkullyModuleGateTest() {
        ReflectionTestUtils.setField(service, "defaultCountryCode", "232");
        var on = mock(Parent.class);
        when(on.getId()).thenReturn("P1");
        when(on.getSchoolId()).thenReturn("ON");
        var off = mock(Parent.class);
        when(off.getId()).thenReturn("P2");
        when(off.getSchoolId()).thenReturn("OFF");
        when(parents.findAllByPhoneDigits(anyCollection())).thenReturn(List.of(on, off));
        for (String id : List.of("ON", "OFF")) {
            var school = mock(School.class);
            when(school.getName()).thenReturn(id + " School");
            when(schools.findById(id)).thenReturn(Optional.of(school));
        }
        when(modules.isEnabled("ON", FeatureModule.WHATSAPP_ASSISTANT)).thenReturn(true);
        when(modules.isEnabled("OFF", FeatureModule.WHATSAPP_ASSISTANT)).thenReturn(false);
        when(students.findByParentAndSchoolId(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void familyOnlyListsSchoolsWithTheModule() {
        var family = service.family("23277123456");

        assertThat(family.schools()).extracting(SkullyParentService.SchoolFamily::schoolId).containsExactly("ON");
        assertThat(family.notEnabledAt()).containsExactly("OFF School");
        verify(students, never()).findByParentAndSchoolId(eq("P2"), any(), any());
    }

    @Test
    void aSchoolWithoutTheModuleHasNoUpdatesOrChildren() {
        assertThat(service.updates("23277123456", "OFF")).isEmpty();
        assertThat(service.child("23277123456", "any-student")).isEmpty();
        verify(students, never()).findByIdAndSchoolId(any(), eq("OFF"));
    }
}
