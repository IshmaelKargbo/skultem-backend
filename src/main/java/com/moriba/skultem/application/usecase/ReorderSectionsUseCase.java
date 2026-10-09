package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.repository.SectionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Re-ranks class sections (A, B, C) so every class lists them in that order. Same approach as
// ReorderClassesUseCase: the sections sent are handed back the rank numbers they already held,
// smallest first, so ranks stay unique and sections that weren't sent keep theirs.
@Service
@Transactional
@RequiredArgsConstructor
public class ReorderSectionsUseCase {

    private final SectionRepository sectionRepo;

    @AuditLogAnnotation(action = "SECTIONS_REORDERED")
    public void execute(String schoolId, List<String> orderedSectionIds) {
        if (new HashSet<>(orderedSectionIds).size() != orderedSectionIds.size()) {
            throw new RuleException("A section can only appear once in the new order");
        }

        List<Section> sections = new ArrayList<>();
        for (String id : orderedSectionIds) {
            sections.add(sectionRepo.findByIdAndSchoolId(id, schoolId)
                    .orElseThrow(() -> new NotFoundException("Section not found")));
        }

        var ranks = sections.stream().map(Section::getDisplayOrder).sorted().toList();
        // Sections that never got a distinct rank (all 0) would stay tied - spread them out.
        boolean tied = new HashSet<>(ranks).size() != ranks.size();
        for (int i = 0; i < sections.size(); i++) {
            int rank = tied ? i + 1 : ranks.get(i);
            var section = sections.get(i);
            if (section.getDisplayOrder() != rank) {
                section.moveTo(rank);
                sectionRepo.save(section);
            }
        }
    }
}
