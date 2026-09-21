package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentLedgerDTO;
import com.moriba.skultem.application.services.StudentLedgerRowMapper;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * One page of a school's platform fee entries for a year - the fee charged to each student and the
 * payments towards it - narrowed by the same filters as the student ledger (name/admission number,
 * class, type, term), newest first unless asked otherwise. Blank filters are ignored.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ListPlatformFeeEntriesUseCase {

    private final StudentLedgerEntryRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final StudentLedgerRowMapper rowMapper;

    public Page<StudentLedgerDTO> execute(String schoolId, String academicYearId, int page, int size, String search,
            String classId, String type, String termId, boolean oldestFirst) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        var direction = oldestFirst ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(page, size, Sort.by(direction, "paidAt"));

        StudentLedgerEntry.TransactionType entryType = null;
        if (type != null && !type.isBlank()) {
            try {
                entryType = StudentLedgerEntry.TransactionType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown ledger entry type: " + type);
            }
        }

        return repo.searchPlatformEntries(academicYear.getId(), schoolId, search, classId, entryType, termId, pageable)
                .map(entry -> rowMapper.toDTO(entry, schoolId));
    }
}
