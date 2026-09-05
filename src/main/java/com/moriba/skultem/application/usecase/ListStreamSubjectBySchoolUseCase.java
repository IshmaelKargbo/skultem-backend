package com.moriba.skultem.application.usecase;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StreamSubjectDTO;
import com.moriba.skultem.application.mapper.StreamSubjctMapper;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStreamSubjectBySchoolUseCase {
    private final StreamSubjectRepository repo;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // ss.<field>", so an unchecked client value would let someone probe/sort by arbitrary entity
    // fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "stream.name", "subject.name");

    public Page<StreamSubjectDTO> execute(String schoolId, int page, int size) {
        return execute(schoolId, page, size, null, null, null, null);
    }

    public Page<StreamSubjectDTO> execute(String schoolId, int page, int size, String streamId, String query) {
        return execute(schoolId, page, size, streamId, query, null, null);
    }

    public Page<StreamSubjectDTO> execute(String schoolId, int page, int size, String streamId, String query,
            String sortBy, String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = Pageable.unpaged(sort);
        if (size > 0) {
            pageable = PageRequest.of(page, size, sort);
        }

        boolean hasFilters = (streamId != null && !streamId.isBlank()) || (query != null && !query.isBlank());
        var subjects = hasFilters
                ? repo.search(schoolId, normalize(streamId), normalize(query), pageable)
                : repo.findBySchoolId(schoolId, pageable);

        return subjects.map(StreamSubjctMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    // Empty string, never null - see the repository's search query for why.
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
