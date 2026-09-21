package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * The platform-wide academic calendar (Sierra Leone's school year and the terms the ministry
 * publishes), entered once by a SYSTEM_ADMIN instead of by every school. Only one year is
 * {@link #isCurrent() current} at a time - that's the one a newly-created school starts from (see
 * ProvisionAcademicCalendarForNewSchoolUseCase). A school gets a copy of it, not a link: editing
 * this later never changes a school's own year/terms.
 */
@Getter
public class NationalAcademicYear extends AggregateRoot<String> {

    public static final int MAX_TERMS = 3;

    public record NationalTerm(int termNumber, String name, LocalDate startDate, LocalDate endDate) {
    }

    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean current;
    private List<NationalTerm> terms;

    public NationalAcademicYear(String id, String name, LocalDate startDate, LocalDate endDate, boolean current,
            List<NationalTerm> terms, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.current = current;
        this.terms = terms;
        touch(updatedAt);
    }

    public static NationalAcademicYear create(String id, String name, LocalDate startDate, LocalDate endDate,
            List<NationalTerm> terms) {
        Instant now = Instant.now();
        return new NationalAcademicYear(id, name, startDate, endDate, true, terms, now, now);
    }

    public void update(LocalDate startDate, LocalDate endDate, List<NationalTerm> terms) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.terms = terms;
        this.current = true;
        touch(Instant.now());
    }

    public void unsetCurrent() {
        this.current = false;
        touch(Instant.now());
    }
}
