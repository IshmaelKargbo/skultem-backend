package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.LibraryBook;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.SectionScope;

class LibrarySectionAccessTest {

    private final SectionScopeService scopes = mock(SectionScopeService.class);
    private final ManagementSectionRepository sectionRepo = mock(ManagementSectionRepository.class);
    private final SchoolRepository schoolRepo = mock(SchoolRepository.class);
    private final LibrarySectionAccess access = new LibrarySectionAccess(scopes, sectionRepo, schoolRepo);

    @BeforeEach
    void setUp() {
        var primary = mock(ManagementSection.class);
        when(primary.getId()).thenReturn("PRIMARY");
        var secondary = mock(ManagementSection.class);
        when(secondary.getId()).thenReturn("SECONDARY");
        when(sectionRepo.findBySchoolId("SCL-1")).thenReturn(List.of(primary, secondary));
        var school = mock(School.class);
        when(school.getManagementModel()).thenReturn(ManagementModel.SECTION_BASED);
        when(schoolRepo.findById("SCL-1")).thenReturn(Optional.of(school));
        when(scopes.view()).thenReturn(Optional.empty());
    }

    private void wholeSchool() {
        when(scopes.currentOrAll()).thenReturn(SectionScope.all());
    }

    private void limitedTo(String... sectionIds) {
        when(scopes.currentOrAll()).thenReturn(
                SectionScope.sections(EnumSet.of(Level.PRIMARY), List.of(sectionIds), List.of(sectionIds)));
    }

    private static LibraryBook book(String sectionId) {
        var book = mock(LibraryBook.class);
        when(book.getSectionId()).thenReturn(sectionId);
        return book;
    }

    @Test
    void wholeSchoolStaffSeeEveryBookIncludingUnassignedOnes() {
        wholeSchool();
        assertThat(access.visibleSections()).isNull();
        assertThat(access.canSee(book("SECONDARY"), null)).isTrue();
        assertThat(access.canSee(book(null), null)).isTrue();
    }

    @Test
    void sectionStaffSeeOnlyTheirSectionsBooks() {
        limitedTo("PRIMARY");
        var visible = access.visibleSections();
        assertThat(visible).containsExactly("PRIMARY");
        assertThat(access.canSee(book("PRIMARY"), visible)).isTrue();
        assertThat(access.canSee(book("SECONDARY"), visible)).isFalse();
        assertThat(access.canSee(book(null), visible)).isFalse();
        assertThatThrownBy(() -> access.requireVisible(book("SECONDARY"))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void anOwnerViewingOneSectionSeesOnlyThatSection() {
        wholeSchool();
        when(scopes.view()).thenReturn(Optional.of(
                SectionScope.sections(EnumSet.of(Level.PRIMARY), List.of("SECONDARY"), List.of("Secondary"))));
        assertThat(access.visibleSections()).containsExactly("SECONDARY");
    }

    @Test
    void staffWithOneSectionDontHaveToPickIt() {
        limitedTo("PRIMARY");
        assertThat(access.sectionForSave("SCL-1", null)).isEqualTo("PRIMARY");
    }

    @Test
    void sectionStaffCantPutBooksInAnotherSection() {
        limitedTo("PRIMARY");
        assertThatThrownBy(() -> access.sectionForSave("SCL-1", "SECONDARY")).isInstanceOf(RuleException.class);
    }

    @Test
    void aSectionBasedSchoolNeedsASectionOnEveryBook() {
        wholeSchool();
        assertThatThrownBy(() -> access.sectionForSave("SCL-1", "")).isInstanceOf(RuleException.class);
        assertThat(access.sectionForSave("SCL-1", "SECONDARY")).isEqualTo("SECONDARY");
        assertThatThrownBy(() -> access.sectionForSave("SCL-1", "NOPE")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void aSchoolNotRunBySectionsKeepsBooksWithoutOne() {
        wholeSchool();
        var school = mock(School.class);
        when(school.getManagementModel()).thenReturn(null);
        when(schoolRepo.findById("SCL-1")).thenReturn(Optional.of(school));
        assertThat(access.sectionForSave("SCL-1", null)).isNull();
    }
}
