package com.moriba.skultem.application.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.LibraryBook;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

// The library is split by management section: every book belongs to one, and staff limited to sections (an admin,
// principal or librarian given "Primary", say) only see, edit and lend their sections' books. Whole-school staff see
// everything - or one section, while an owner has picked it in the header. A book with no section yet is only seen
// by whole-school staff, until someone assigns it.
@Service
@RequiredArgsConstructor
public class LibrarySectionAccess {

    private final SectionScopeService scopeService;
    private final ManagementSectionRepository sectionRepo;
    private final SchoolRepository schoolRepo;

    /** Section ids whose books the caller sees; null means every book in the school. */
    @Transactional(readOnly = true)
    public List<String> visibleSections() {
        var scope = scopeService.view().orElseGet(scopeService::currentOrAll);
        return scope.wholeSchool() ? null : scope.sectionIds();
    }

    public boolean canSee(LibraryBook book, List<String> visible) {
        return visible == null || (book.getSectionId() != null && visible.contains(book.getSectionId()));
    }

    /** Refuses a book outside the caller's sections as if it didn't exist. */
    public void requireVisible(LibraryBook book) {
        if (!canSee(book, visibleSections())) {
            throw new NotFoundException("Book not found");
        }
    }

    /**
     * The section a new or edited book goes in. Staff with a single section don't have to pick it. A school run by
     * sections needs one on every book; a school that isn't keeps books without one.
     */
    @Transactional(readOnly = true)
    public String sectionForSave(String schoolId, String requested) {
        var visible = visibleSections();
        String sectionId = requested == null || requested.isBlank() ? null : requested.trim();
        if (sectionId == null && visible != null && visible.size() == 1) {
            sectionId = visible.get(0);
        }

        var sections = sectionRepo.findBySchoolId(schoolId);
        if (sectionId == null) {
            boolean sectionBased = schoolRepo.findById(schoolId)
                    .map(s -> s.getManagementModel() == ManagementModel.SECTION_BASED).orElse(false);
            if (sectionBased && !sections.isEmpty()) {
                throw new RuleException("Choose the section this book belongs to");
            }
            return null;
        }

        String chosen = sectionId;
        if (sections.stream().noneMatch(s -> s.getId().equals(chosen))) {
            throw new NotFoundException("Section not found");
        }
        if (visible != null && !visible.contains(chosen)) {
            throw new RuleException("You can only manage books in your own sections");
        }
        return chosen;
    }
}
