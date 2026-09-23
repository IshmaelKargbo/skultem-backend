package com.moriba.skultem.application.usecase;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.PlaygroundDataCategory;
import com.moriba.skultem.domain.repository.PlaygroundDataRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Takes a playground school live: clears whichever categories of test data the school picked
 * (see {@link PlaygroundDataCategory}) and turns playground mode off. An empty selection is valid -
 * a school that entered real data while trying things out can keep all of it.
 *
 * The school's SETUP is never touched (classes, sections, streams, subjects, fee structures,
 * academic years/terms, grading scale, branding, teachers/staff, materials catalogue) - that's
 * configuration the school did on purpose and would otherwise have to redo before going live.
 *
 * Gated on {@link com.moriba.skultem.domain.model.School#isTestSchool()} - a live school can never
 * be wiped through this use case, so this can't be pointed at live data by a wrong id - and on the
 * caller typing the school's domain back, so a stray request can't trigger it either. One
 * transaction, all-or-nothing: a partial wipe (orphaned rows left behind) would be worse than none.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class WipeTestSchoolDataUseCase {

    private final SchoolRepository schoolRepo;
    private final PlaygroundDataRepository playgroundDataRepo;

    // @AuditLogAnnotation is Spring-AOP-proxy-based, so it only fires through an external call to
    // this public method - it can't be moved onto a private helper called from within this same
    // class (self-invocation bypasses the proxy entirely and the aspect would silently never run).
    @AuditLogAnnotation(action = "TEST_SCHOOL_DATA_WIPED")
    public SchoolDTO execute(String schoolId, Collection<PlaygroundDataCategory> selected, String confirmation) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));

        if (!school.isTestSchool()) {
            throw new RuleException("This school is already live - only a playground school can be taken live");
        }

        if (confirmation == null || !confirmation.trim().equalsIgnoreCase(school.getDomain())) {
            throw new RuleException("Type the school's domain (" + school.getDomain() + ") to confirm going live");
        }

        var categories = PlaygroundDataCategory.withDependencies(selected == null ? List.of() : selected);
        if (!categories.isEmpty()) {
            playgroundDataRepo.wipe(schoolId, categories);
        }

        school.markAsTest(false);
        schoolRepo.save(school);

        return SchoolMapper.toDTO(school);
    }
}
