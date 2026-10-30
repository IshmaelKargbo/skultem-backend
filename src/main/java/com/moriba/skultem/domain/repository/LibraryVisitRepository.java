package com.moriba.skultem.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.LibraryVisit;

public interface LibraryVisitRepository {

    /** The visit list's filter. UNRESOLVED = still open after the student's school day ended. */
    enum Filter {
        ALL, OPEN, UNRESOLVED, CHECKED_OUT, RESOLVED
    }

    /**
     * Saves straight away, so a second open visit for the same student (two check-ins at the same moment)
     * is refused here by the database - as a DataIntegrityViolationException - rather than at commit.
     */
    void save(LibraryVisit domain);

    Optional<LibraryVisit> findByIdAndSchoolForUpdate(String id, String schoolId);

    List<LibraryVisit> findOpenByStudents(String schoolId, Collection<String> studentIds);

    /**
     * Visits checked in between {@code from} (inclusive) and {@code to} (exclusive), newest first.
     * Null bounds mean "no limit"; an empty query and a null student mean "everyone". The query matches the
     * student's name or class.
     */
    Page<LibraryVisit> search(String schoolId, Instant from, Instant to, String studentId, String query,
            Filter filter, Instant now, Pageable pageable);

    long countOpen(String schoolId);

    long countUnresolved(String schoolId, Instant now);
}
