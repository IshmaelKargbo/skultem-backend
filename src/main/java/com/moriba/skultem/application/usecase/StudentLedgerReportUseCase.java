package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentLedgerReportDTO;
import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentLedgerReportUseCase {

        private final StudentLedgerEntryRepository repo;

        public StudentLedgerReportDTO calculateReport(String schoolId) {

                List<StudentLedgerEntry> entries = repo.findAllBySchoolIdOrderByPaidAtDesc(schoolId, Pageable.unpaged())
                                .getContent();

                BigDecimal totalDebit = entries.stream()
                                .map(StudentLedgerEntry::getDebit)
                                .filter(d -> d != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalCredit = entries.stream()
                                .map(StudentLedgerEntry::getCredit)
                                .filter(c -> c != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal netBalance = totalDebit.subtract(totalCredit);

                return new StudentLedgerReportDTO(totalDebit.longValue(), totalCredit.longValue(), netBalance.longValue());
        }
}