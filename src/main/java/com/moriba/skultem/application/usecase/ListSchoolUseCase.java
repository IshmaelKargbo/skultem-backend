package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.SchoolRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSchoolUseCase {

    private final SchoolRepository repo;

    public Page<SchoolDTO> execute(int page, int size, String query, String status, String type) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var schools = repo.filter(query == null ? "" : query.trim(), parseStatus(status), parseType(type), pageable);

        return schools.map(SchoolMapper::toDTO);
    }

    private static com.moriba.skultem.domain.model.School.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return com.moriba.skultem.domain.model.School.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // "PLAYGROUND" = schools still being tried out, "LIVE" = the rest.
    private static Boolean parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return switch (type.trim().toUpperCase()) {
            case "PLAYGROUND" -> true;
            case "LIVE" -> false;
            default -> null;
        };
    }
}
