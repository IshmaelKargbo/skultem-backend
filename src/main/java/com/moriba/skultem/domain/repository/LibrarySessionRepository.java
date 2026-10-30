package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.LibrarySession;

public interface LibrarySessionRepository {

    /**
     * Saves straight away, so a second session for the same timetable entry and date is refused by the database
     * (a DataIntegrityViolationException) rather than at commit.
     */
    void save(LibrarySession domain);

    Optional<LibrarySession> findByIdAndSchool(String id, String schoolId);

    // Locks the session row, so marking, finalizing, cancelling and correcting one session happen one at a time.
    Optional<LibrarySession> findByIdAndSchoolForUpdate(String id, String schoolId);

    Optional<LibrarySession> findByEntryAndDate(String schoolId, String timetableEntryId, LocalDate date);

    List<LibrarySession> findBetween(String schoolId, LocalDate from, LocalDate to);

    List<LibrarySession> findByIds(String schoolId, Collection<String> ids);
}
