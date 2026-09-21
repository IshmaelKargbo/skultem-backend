package com.moriba.skultem.domain.repository;

import java.util.Optional;

import com.moriba.skultem.domain.model.NationalAcademicYear;

public interface NationalCalendarRepository {
    // Saving a year that's marked current also un-marks whichever one was current before, so there
    // is never more than one.
    void save(NationalAcademicYear domain);

    Optional<NationalAcademicYear> findCurrent();

    Optional<NationalAcademicYear> findByName(String name);
}
