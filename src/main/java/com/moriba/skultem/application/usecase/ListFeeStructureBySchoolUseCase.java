package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.vo.Gender;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListFeeStructureBySchoolUseCase {

    private final SectionScopeService sectionScopeService;
    private final FeeStructureRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // f.<field>" (see FeeStructureJpaRepository.search), so an unchecked client value would let
    // someone probe/sort by arbitrary entity fields.
    private static final Set<String> SORTABLE_FIELDS = Set.of("amount", "dueDate", "createdAt");

    /**
     * The fee structures of one academic year - the one asked for, or the school's active year when none is
     * given - not every structure the school has ever set up. A school with no active year has none to show
     * (an empty page, not an error); a year that was asked for but doesn't exist is.
     */
    public Page<FeeStructureDTO> execute(String schoolId, String academicYearId, int page, int size, String termId,
            String classId, Boolean newStudentsOnly, Boolean oldStudentsOnly, Gender gender, String sortBy,
            String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);
        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }

        String yearId;
        try {
            yearId = resolveAcademicYearUseCase.execute(schoolId, academicYearId).getId();
        } catch (NotFoundException e) {
            if (academicYearId != null && !academicYearId.isBlank()) {
                throw e;
            }
            return Page.empty(pageable);
        }

        return repo.search(schoolId, yearId, termId, classId, newStudentsOnly, oldStudentsOnly, gender,
                sectionScopeService.levels(), pageable)
                .map(FeeStructureMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        // Set.of(...) throws NPE from .contains(null), not just "false" like a HashSet would -
        // sortBy is null whenever a caller doesn't pass it at all, so that null check must come
        // first.
        String field = (sortBy != null && SORTABLE_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }
}
