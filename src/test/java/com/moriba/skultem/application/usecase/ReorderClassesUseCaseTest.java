package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.vo.Level;

@ExtendWith(MockitoExtension.class)
class ReorderClassesUseCaseTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private ClassRepository classRepo;
    @Mock
    private SectionScopeService sectionScopeService;
    @InjectMocks
    private ReorderClassesUseCase useCase;

    private Clazz jss1;
    private Clazz jss2;
    private Clazz jss3;

    @BeforeEach
    void setUp() {
        jss1 = Clazz.create("c1", SCHOOL, null, "JSS 1", Level.JSS, 7);
        jss2 = Clazz.create("c2", SCHOOL, null, "JSS 2", Level.JSS, 8);
        jss3 = Clazz.create("c3", SCHOOL, null, "JSS 3", Level.JSS, 9);
        for (var c : List.of(jss1, jss2, jss3)) {
            lenient().when(classRepo.findByIdAndSchool(c.getId(), SCHOOL)).thenReturn(Optional.of(c));
        }
        lenient().when(sectionScopeService.levels()).thenReturn(EnumSet.allOf(Level.class));
    }

    @Test
    void hands_the_same_ranks_back_in_the_new_order() {
        useCase.execute(SCHOOL, List.of("c3", "c1", "c2"));

        assertThat(jss3.getDisplayOrder()).isEqualTo(7);
        assertThat(jss1.getDisplayOrder()).isEqualTo(8);
        assertThat(jss2.getDisplayOrder()).isEqualTo(9);
    }

    @Test
    void only_saves_classes_that_moved() {
        useCase.execute(SCHOOL, List.of("c1", "c3", "c2"));

        verify(classRepo, never()).save(jss1);
        verify(classRepo).save(jss3);
        verify(classRepo).save(jss2);
    }

    @Test
    void rejects_a_class_listed_twice() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, List.of("c1", "c1")))
                .isInstanceOf(RuleException.class);
        verify(classRepo, never()).save(any());
    }

    @Test
    void rejects_an_unknown_class() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, List.of("c1", "nope")))
                .isInstanceOf(NotFoundException.class);
        verify(classRepo, never()).save(any());
    }

    @Test
    void rejects_a_class_outside_the_callers_management_scope() {
        when(sectionScopeService.levels()).thenReturn(EnumSet.of(Level.PRIMARY));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, List.of("c1", "c2")))
                .isInstanceOf(RuleException.class);
        verify(classRepo, never()).save(any());
    }
}
