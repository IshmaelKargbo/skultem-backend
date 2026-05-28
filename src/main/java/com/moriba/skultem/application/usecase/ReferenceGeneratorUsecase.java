package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Year;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ReferenceSequence;
import com.moriba.skultem.domain.repository.ReferenceSequenceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReferenceGeneratorUsecase {
    private final ReferenceSequenceRepository rs;
    private static final DateTimeFormatter TIME_SERIES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    @Transactional
    public String generate(String referenceType, String prefix) {

        int year = Year.now().getValue();

        var res = rs.findForUpdate(referenceType, year);
        var sequence = res.orElseThrow(() -> new NotFoundException("Reference sequence not found"));

        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);

        rs.save(sequence);

        return format(prefix, year, nextNumber);
    }

    @Transactional
    public String generateForSchool(String referenceType, String prefix, String schoolId) {
        int year = Year.now().getValue();
        var res = rs.findForUpdate(referenceType + ":" + schoolId, year);
        var sequence = res.orElseThrow(() -> new NotFoundException("Reference sequence not found"));
        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        rs.save(sequence);
        return format(prefix, year, nextNumber);
    }

    @Transactional
    public String generateTimeSeriesForSchool(String referenceType, String prefix, String schoolId) {
        int year = Year.now(ZoneOffset.UTC).getValue();
        var res = rs.findForUpdate(referenceType + ":" + schoolId, year);
        var sequence = res.orElseThrow(() -> new NotFoundException("Reference sequence not found"));

        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        rs.save(sequence);

        return String.format("%s-%s-%04d", prefix, TIME_SERIES_FORMATTER.format(Instant.now()), nextNumber);
    }

    @Transactional
    public ReferenceSequence generateWithDomain(String referenceType) {

        int year = Year.now().getValue();

        var res = rs.findForUpdate(referenceType, year);
        var sequence = res.orElseThrow(() -> new NotFoundException("Reference sequence not found"));

        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);

        rs.save(sequence);

        return sequence;
    }

    private String format(String prefix, int year, int number) {
        return String.format("%s-%d-%04d", prefix, year, number);
    }
}
