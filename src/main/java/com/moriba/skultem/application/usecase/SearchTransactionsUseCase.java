package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TransactionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TransactionMapper;
import com.moriba.skultem.domain.model.Transaction.Direction;
import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.model.Transaction.TransactionType;
import com.moriba.skultem.domain.repository.TransactionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The Transactions page's listing: a school's transactions for one academic year - the one asked for, or
 * the school's active year when none is given - newest first unless asked otherwise, narrowed by any of
 * type, direction, what they relate to, and a date range. Blank filters are ignored. An unknown
 * type/direction/reference value is a 400 rather than silently matching nothing. A school with no active
 * year has no transactions to show (an empty page, not an error); a year that was asked for but doesn't
 * exist is an error.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SearchTransactionsUseCase {

    private final TransactionRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public Page<TransactionDTO> execute(String schoolId, String academicYearId, int page, int size, String type,
            String direction, String referenceType, LocalDate from, LocalDate to, boolean oldestFirst) {

        var sort = Sort.by(oldestFirst ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
        var pageable = PageRequest.of(page, size, sort);

        String yearId;
        try {
            yearId = resolveAcademicYearUseCase.execute(schoolId, academicYearId).getId();
        } catch (NotFoundException e) {
            if (academicYearId != null && !academicYearId.isBlank()) {
                throw e;
            }
            return Page.empty(pageable);
        }

        // Whole days in the school's timezone (the JVM is pinned to it - see SkultemApplication); the
        // end date is inclusive, so the range runs up to the start of the day after it.
        var zone = ZoneId.systemDefault();
        var fromInstant = from != null ? from.atStartOfDay(zone).toInstant() : null;
        var toExclusive = to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : null;

        return repo.searchTransactions(
                schoolId,
                yearId,
                parse(TransactionType.class, type, "transaction type"),
                parse(Direction.class, direction, "direction"),
                parse(ReferenceType.class, referenceType, "reference type"),
                fromInstant,
                toExclusive,
                pageable).map(TransactionMapper::toDTO);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String what) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown " + what + ": " + value);
        }
    }
}
