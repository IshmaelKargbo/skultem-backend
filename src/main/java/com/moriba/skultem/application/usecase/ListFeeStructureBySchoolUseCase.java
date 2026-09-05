package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.mapper.FeeStructureMapper;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListFeeStructureBySchoolUseCase {
    private final FeeStructureRepository repo;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // f.<field>" (see FeeStructureJpaRepository.search), so an unchecked client value would let
    // someone probe/sort by arbitrary entity fields.
    private static final Set<String> SORTABLE_FIELDS = Set.of("amount", "dueDate", "createdAt");

    public Page<FeeStructureDTO> execute(String schoolId, int page, int size, String termId, String classId,
            Boolean newStudentsOnly, Boolean oldStudentsOnly, String sortBy, String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);
        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }
        return repo.search(schoolId, termId, classId, newStudentsOnly, oldStudentsOnly, pageable)
                .map(FeeStructureMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }
}
