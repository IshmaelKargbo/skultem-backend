package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.NationalAcademicYear;
import com.moriba.skultem.domain.model.NationalAcademicYear.NationalTerm;
import com.moriba.skultem.domain.repository.NationalCalendarRepository;
import com.moriba.skultem.infrastructure.persistence.entity.NationalAcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.entity.NationalTermEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.NationalAcademicYearJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.NationalTermJpaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NationalCalendarAdapter implements NationalCalendarRepository {

    private final NationalAcademicYearJpaRepository yearRepo;
    private final NationalTermJpaRepository termRepo;

    @Override
    @Transactional
    public void save(NationalAcademicYear domain) {
        if (domain.isCurrent()) {
            // Clear the previous current year first so the "only one current" invariant holds
            // even if two saves race.
            yearRepo.findFirstByCurrentTrue()
                    .filter(e -> !e.getId().equals(domain.getId()))
                    .ifPresent(e -> {
                        e.setCurrent(false);
                        yearRepo.saveAndFlush(e);
                    });
        }

        yearRepo.save(NationalAcademicYearEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .current(domain.isCurrent())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build());

        // Terms are replaced wholesale - there are at most three, and they're edited as one unit.
        termRepo.deleteAllByNationalAcademicYearId(domain.getId());
        termRepo.flush();
        termRepo.saveAll(domain.getTerms().stream()
                .map(t -> NationalTermEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .nationalAcademicYearId(domain.getId())
                        .termNumber(t.termNumber())
                        .name(t.name())
                        .startDate(t.startDate())
                        .endDate(t.endDate())
                        .build())
                .toList());
    }

    @Override
    public Optional<NationalAcademicYear> findCurrent() {
        return yearRepo.findFirstByCurrentTrue().map(this::toDomain);
    }

    @Override
    public Optional<NationalAcademicYear> findByName(String name) {
        return yearRepo.findByNameIgnoreCase(name).map(this::toDomain);
    }

    private NationalAcademicYear toDomain(NationalAcademicYearEntity e) {
        var terms = termRepo.findAllByNationalAcademicYearIdOrderByTermNumberAsc(e.getId()).stream()
                .map(t -> new NationalTerm(t.getTermNumber(), t.getName(), t.getStartDate(), t.getEndDate()))
                .toList();
        return new NationalAcademicYear(e.getId(), e.getName(), e.getStartDate(), e.getEndDate(), e.isCurrent(),
                terms, e.getCreatedAt(), e.getUpdatedAt());
    }
}
